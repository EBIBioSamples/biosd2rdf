package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertAnnotationData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertClass;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertIndividual;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertLink;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ebi.fg.biosd.biosd2rdf.utils.BioSdOntologyTermResolver;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.Unit;
import uk.ac.ebi.fg.core_model.toplevel.Accessible;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mapping.properties.PropertyRdfMapper;

/**
 * Maps a sample property like 'Characteristics[organism]' to proper RDF/OWL statements. OBI and other relevant ontologies
 * are used for that. 
 *
 * <dl><dt>date</dt><dd>Apr 29, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
@SuppressWarnings ( "rawtypes" )
public class ExpPropValueRdfMapper<T extends Accessible> extends PropertyRdfMapper<T, ExperimentalPropertyValue>
{
	private static BioSdOntologyTermResolver otermResolver = new BioSdOntologyTermResolver ();
	
	public ExpPropValueRdfMapper ()
	{
		super ();
	}

	/**
	 * Mapping examples:
	 * 
	 * TODO
	 * 
	 */
	@Override
	public boolean map ( T sample, ExperimentalPropertyValue pval, Map<String, Object> params )
	{
		try
		{
			// TODO: warnings
			if ( pval == null ) return false;
						
			String sampleAcc = StringUtils.trimToNull ( sample.getAcc () );
			if ( sampleAcc == null ) return false;

			String valLabel = StringUtils.trimToNull ( pval.getTermText () );
			if ( valLabel == null ) return false;
					
			Unit unit = pval.getUnit ();
			if ( unit != null ) {
				String ulabel = StringUtils.trimToNull ( unit.getTermText () );
				// TODO: unit ontology
				if ( ulabel != null ) valLabel += " " + ulabel;
			}
					
			// Process the type
			// 
			ExperimentalPropertyType ptype = pval.getType ();
			String typeLabel = BioSdOntologyTermResolver.getExpPropTypeLabel ( ptype );
			if ( typeLabel == null ) return false;
			
			// TODO: is this the same as getAcc() or a secondary accession? 
			if ( "sample accession".equalsIgnoreCase ( typeLabel ) ) return false;

			RdfMapperFactory mapFact = this.getMapperFactory ();
			OWLOntology onto = mapFact.getKnowledgeBase ();
			
			// name -> dc:title
			if ( typeLabel != null && typeLabel.toLowerCase ().matches ( "(sample |group |sample group |)?name" ) )
			{
				assertData ( onto, mapFact.getUri ( sample ), ns ( "dc-terms", "title" ), valLabel );
				assertData ( onto, mapFact.getUri ( sample ), ns ( "rdfs", "label" ), valLabel );
				return true;
			}

			// TODO: description
			
			String valUri = null, typeUri = null;

			// Find a suitable parent accession for properties. If a submission accession is available for this sample,
			// which is passed by either the MSI mapper or the Sample Group mapper, then uses such accession to build the 
			// sample properties URIs, i.e. makes those with common labels unique within the same submission.
			// 
			// In the unlikely case that such MSI accession cannot be recovered here, then use the sample accession itself 
			// as a base for property URIs and that means that each sample will have RDF-wise different properties, even
			// when they share labels. See below for details.
			//
			// TODO: should be correct to share among submissions, to be checked.
			//
			String parentAcc = params == null ? null : (String) params.get ( "msiAccession" );
			if ( parentAcc == null ) parentAcc = sampleAcc;
			parentAcc = urlEncode ( parentAcc );
			
			
			// Define the property value
			valUri = ns ( "biosd", "exp-prop-val/" + parentAcc + "#" + hashUriSignature ( typeLabel + valLabel ) ); 
			assertData ( onto, valUri, ns ( "dc-terms", "title" ), valLabel );
			assertData ( onto, valUri, ns ( "rdfs", "label" ), valLabel );
			
			
			// Define a type URI that is specific to this type and generically subclass of efo:experimental factor
			typeUri = ns ( "biosd", "exp-prop-type/" + parentAcc + "#" + hashUriSignature (  typeLabel ) );
			
			// Define such type as a new class
			assertClass ( onto, typeUri, ns ( "efo", "EFO_0000001" ) ); // Experimental factor
			assertAnnotationData ( onto, typeUri, ns ( "rdfs", "label" ), typeLabel );
			assertAnnotationData ( onto, typeUri, ns ( "dc-terms", "title" ), typeLabel );
			
			// So, we have: *** propValue a typeUri ***, where the type URI is either a new URI achieved from the type label (essentially 
			// a type unknown to standard ontologies), or a proper OWL class from a standard ontology, tracked by Zooma.
			assertIndividual ( onto, valUri, typeUri );

			// Ask ontology term discoverer (eg, Zooma) if it has a known ontology term for typeLabel
			// TODO: values and units too.
			String discoveredTypeUri = otermResolver.getOntoClassUri ( pval );
			// If you discovered something, add it as a type to the current property value
			if ( discoveredTypeUri != null ) assertIndividual ( onto, valUri, discoveredTypeUri );
			
			
			// Define the link to the type
			String attributeLinkUri = pval instanceof BioCharacteristicValue  
				? ns ( "biosd-terms", "has-bio-characteristic" ) // sub-property of obo:IAO_0000136 (is_about) 
				: ns ( "obo", "IAO_0000136" );	// is about
				
			// Now we have either *** sample has-biocharacteristic valUri ***, or *** sample is-about valUri ***, depending on 
			// the BioSD property type. has-biocharacteristic is a subproperty of is-about.
			assertLink ( onto, mapFact.getUri ( sample ), attributeLinkUri, valUri );
			
			return true;
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Error while mapping sample '%s'.[%s]=[%s]: '%s'", 
				sample.getAcc (), pval.getType (), pval, ex.getMessage () ), 
			ex );
		}
	}
	
}
