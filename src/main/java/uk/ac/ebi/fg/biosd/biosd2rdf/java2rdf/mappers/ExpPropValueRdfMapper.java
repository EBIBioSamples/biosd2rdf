package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.Unit;
import uk.ac.ebi.fg.core_model.toplevel.Accessible;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mappers.PropertyRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils;
import uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils;
import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.*;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.*;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Apr 29, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
@SuppressWarnings ( "rawtypes" )
public class ExpPropValueRdfMapper<T extends Accessible> extends PropertyRdfMapper<T, ExperimentalPropertyValue>
{
	public ExpPropValueRdfMapper ()
	{
		super ();
	}

	
	@Override
	public boolean map ( T source, ExperimentalPropertyValue pval )
	{
		// TODO: warnings
		if ( source == null || pval == null ) return false;
					
		String srcAcc = StringUtils.trimToNull ( source.getAcc () );
		if ( srcAcc == null ) return false;

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
		String typeStr = StringUtils.trimToNull ( ptype.getTermText () );
		if ( typeStr == null ) return false;
		
		if ( "sample accession".equalsIgnoreCase ( typeStr ) ) return false;

		BeanRdfMapperFactory mapFact = this.getMapperFactory ();
		OWLOntology onto = mapFact.getKnowledgeBase ();
		
		if ( "name".equalsIgnoreCase ( typeStr ) ) {
			assertData ( onto, mapFact.getUri ( source ), ns ( "dc", "title" ), valLabel );
			return true;
		}

		// TODO: description
		
		
		
		
		String valUri = null, typeUri = null; boolean isZoomaUri = false;
		
		if ( isZoomaUri )
			// If it's a known term, assume same label and same type label correspond to the same property value and type
			valUri = ns ( "biosd", "exp-prop-val/" + hashUriSignature ( typeStr + valLabel ) );
		else 
		{
			// else, share label+typeLabel over the same submission
			// TODO: should be correct to share among submissions, to be checked.
			//
			Set<MSI> msis = source instanceof BioSample 
				? ((BioSample) source).getMSIs () 
				: source instanceof BioSampleGroup 
					? ((BioSampleGroup) source).getMSIs () : null;
			if ( msis != null && msis.size () == 1 ) {
				String msiAcc = StringUtils.trimToNull ( msis.iterator ().next ().getAcc () );
				if ( msiAcc != null ) srcAcc = msiAcc;  
			}
			valUri = ns ( "biosd", "exp-prop-val/" + srcAcc + "/" + hashUriSignature ( typeStr + valLabel ) ); 
			typeUri = ns ( "biosd", "exp-prop-type/" + srcAcc + "/" + hashUriSignature (  typeStr ) );
		}
		
		// Define the property value
		assertIndividual ( onto, valUri, ns ( "obo", "BFO_0000019" ) ); // Quality
		assertData ( onto, valUri, ns ( "dc", "title" ), valLabel );		

		// Define the Type
		assertIndividual ( onto, typeUri, ns ( "biosd", "PropertyType" ) ); // TODO: define as IAO_0000030 (information content entity)
		assertData ( onto, typeUri, ns ( "dc", "title" ), typeStr );
		
		// TODO: if there is a Zooma URI for this type, make the type an instance of such URI
		// OR SOMETHING ELSE?! REVIEW EFO
		
		// Define the link to the type
		String typeLinkUri = pval instanceof BioCharacteristicValue  
			? ns ( "biosd", "has_characteristic_type" ) // TODO: needs to be defined as sub-property of obo:IAO_0000136 (is_about) 
			: ns ( "obo", "IAO_0000136" );	// is about
		assertLink ( onto, valUri, typeLinkUri, typeUri );
		
		// Link the source to the prop-value
		assertLink ( onto, mapFact.getUri ( source ), ns ( "obo", "BFO_0000086" ), valUri ); // has quality at some time

		return true;
	}

	
}
