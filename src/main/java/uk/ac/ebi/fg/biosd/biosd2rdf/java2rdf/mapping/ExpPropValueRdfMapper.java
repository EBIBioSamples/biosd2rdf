package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertClass;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertIndividual;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertLink;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ebi.fg.biosd.biosd2rdf.utils.BioSdOntologyTermResolver;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.Unit;
import uk.ac.ebi.fg.core_model.toplevel.Accessible;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mapping.properties.PropertyRdfMapper;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;

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
	public boolean map ( T sample, ExperimentalPropertyValue pval )
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
			if ( "name".equalsIgnoreCase ( typeLabel ) ) {
				assertData ( onto, mapFact.getUri ( sample ), ns ( "dc-terms", "title" ), valLabel );
				return true;
			}

			// TODO: description
			
			String valUri = null, typeUri = null;

			// Find a suitable parent accession for properties, to establish the scope (submission, sample group, or sample itself), 
			// under which the properties should be shared (i.e, considered the same if the labels are the same).
			//
			// TODO: should be correct to share among submissions, to be checked.
			//
			String parentAcc = sampleAcc;
			Set<MSI> msis = sample instanceof BioSample 
				? ((BioSample) sample).getMSIs () 
				: sample instanceof BioSampleGroup 
					? ((BioSampleGroup) sample).getMSIs () : null;
			if ( msis != null && msis.size () == 1 ) {
				String msiAcc = StringUtils.trimToNull ( msis.iterator ().next ().getAcc () );
				if ( msiAcc != null ) parentAcc = msiAcc;  
			}
			parentAcc = urlEncode ( parentAcc );
			
			
			// Define the property value
			valUri = ns ( "biosd", "exp-prop-val/" + parentAcc + "#" + hashUriSignature ( typeLabel + valLabel ) ); 
			assertData ( onto, valUri, ns ( "dc-terms", "title" ), valLabel );
			
			
			// Ask Zooma if it has a known ontology term for typeLabel
			// TODO: values and units too.
			typeUri = otermResolver.getOntoClassUri ( pval );
			
			if ( typeUri == null ) 
			{
				// else, share label+typeLabel over the same submission
				typeUri = ns ( "biosd", "exp-prop-type/" + parentAcc + "#" + hashUriSignature (  typeLabel ) );
				
				// Define the Type as a new class
				assertClass ( onto, typeUri, ns ( "efo", "EFO_0000001" ) ); // Experimental factor
				assertData ( onto, typeUri, ns ( "rdfs", "label" ), typeLabel );
			}
					
			// So, we have: *** propValue a typeUri ***, where the type URI is either a new URI achieved from the type label (essentially 
			// a type unknown to standard ontologies), or a proper OWL class from a standard ontology, tracked by Zooma.
			assertIndividual ( onto, valUri, typeUri );
			
			// Define the link to the type
			String attributeLinkUri = pval instanceof BioCharacteristicValue  
				? ns ( "ebi-terms", "has-bio-characteristic" ) // TODO: needs to be defined as sub-property of obo:IAO_0000136 (is_about) 
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
