package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.uri;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertAnnotationData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertIndividual;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertLink;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.xjc.runtime.DataTypeAdapter;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import uk.ac.ebi.bioportal.webservice.client.BioportalClient;
import uk.ac.ebi.bioportal.webservice.model.OntologyClass;
import uk.ac.ebi.bioportal.webservice.model.OntologyClassMapping;
import uk.ac.ebi.fg.biosd.annotator.model.AbstractOntoTermAnnotation;
import uk.ac.ebi.fg.biosd.annotator.model.DataItem;
import uk.ac.ebi.fg.biosd.annotator.model.DateItem;
import uk.ac.ebi.fg.biosd.annotator.model.ExpPropValAnnotation;
import uk.ac.ebi.fg.biosd.annotator.model.NumberItem;
import uk.ac.ebi.fg.biosd.annotator.model.NumberRangeItem;
import uk.ac.ebi.fg.biosd.annotator.ontodiscover.OntoResolverAndAnnotator;
import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.Unit;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.toplevel.Accessible;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mapping.properties.PropertyRdfMapper;
import uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils;

/**
 * Maps a sample property like 'Characteristics[organism]' to proper RDF/OWL statements. OBI and other relevant ontologies
 * are used for that. <a href = 'http://www.ebi.ac.uk/rdf/documentation/biosamples'>Here</a> you can find examples of 
 * what this class produces. 
 *
 * <dl><dt>date</dt><dd>Apr 29, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
@SuppressWarnings ( "rawtypes" )
public class ExpPropValueRdfMapper<T extends Accessible> extends PropertyRdfMapper<T, ExperimentalPropertyValue>
{
	/**
	 * If true, the Bioportal ontology mappings service is used to fetch mappings between ontology terms that are discovered
	 * by ZOOMA.
	 */
	public static final String FETCH_ONTOLOGY_MAPPINGS_PROP_NAME = "uk.ac.ebi.fg.biosd.biosd2rdf.fetchOntologyMappings";
	
	/**
	 * <p>If this is true, the old linked data model is supported, in addition to the new one.
	 * See <a href = 'https://www.ebi.ac.uk/rdf/biosd/newschema16'>here</a> for details.</p>
	 * 
	 */
	public static final boolean OLD_MODEL_SUPPORT_FLAG = false; 
	

	public final boolean fetchOntologyMappings = "true".equals ( 
		System.getProperty ( FETCH_ONTOLOGY_MAPPINGS_PROP_NAME, "false" )
	);

	
	public ExpPropValueRdfMapper () {
		super ();
	}

	@Override
	public boolean map ( T sample, ExperimentalPropertyValue pval, Map<String, Object> params )
	{
		EntityManager entityManager = null;
		
		try
		{
			// TODO: warnings
			if ( pval == null ) return false;
						
			String sampleAcc = StringUtils.trimToNull ( sample.getAcc () );
			if ( sampleAcc == null ) return false;			
			
			// Process the type
			// 
			ExperimentalPropertyType ptype = pval.getType ();
			
			String typeLabel = ExpPropValAnnotation.getExpPropTypeLabel ( ptype );
			if ( typeLabel == null ) return false;
			
			// TODO: is this the same as getAcc() or a secondary accession? 
			// We're ignoring it here, cause the accession is already assigned by the sample mapper.
			if ( "sample accession".equalsIgnoreCase ( typeLabel ) ) return false;

			String typeLabelLC = typeLabel.toLowerCase ();

			// The Sample mapper already deals with these
			if ( "same as".equals ( typeLabel ) 
			     || typeLabelLC.matches ( "derived (from|to)" ) 
			     || typeLabelLC.matches ( "(child|parent) of" ) )
			  return false;
			
			RdfMapperFactory mapFact = this.getMapperFactory ();
			OWLOntology onto = mapFact.getKnowledgeBase ();
			
			// Get label for the value, using the original textual value
			//
			String valueLabel = StringUtils.trimToNull ( pval.getTermText () ); 
						
			// name -> dc:title and similar
			if ( typeLabelLC.matches ( "(sample |group |sample group |)?name" ) )
			{
				assertData ( onto, mapFact.getUri ( sample, params ), uri ( "dc-terms", "title" ), valueLabel );
				assertData ( onto, mapFact.getUri ( sample, params ), uri ( "rdfs", "label" ), valueLabel );
				return true;
			}

			// 'Sample Description' -> dc-terms:description and similar
			if ( typeLabelLC.matches ( "(sample |group |sample group |)?description" ) )
			{
				assertData ( onto, mapFact.getUri ( sample, params ), uri ( "dc-terms", "description" ), valueLabel );
				assertData ( onto, mapFact.getUri ( sample, params ), uri ( "rdfs", "comment" ), valueLabel );
				return true;
			}

			
			// Possibly add the unit to the value label
			//
			Unit unit = pval.getUnit ();
			String unitLabel = unit == null ? null : StringUtils.trimToNull ( unit.getTermText () );
			if ( unitLabel != null ) valueLabel += " " + unitLabel;
										
			
			// Build the attribute URI
			//
			String valUri = null;

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
			
			// Assert the property value
			String pvalHash = hashUriSignature ( typeLabel + valueLabel );
			valUri = uri ( "biosd", "exp-prop-val/" + parentAcc + "#" + pvalHash ); 

			// Assert the value label
			assertData ( onto, valUri, uri ( "dc-terms", "title" ), valueLabel );
			assertData ( onto, valUri, uri ( "rdfs", "label" ), valueLabel );
			
			// A string for the type is always reported
			assertAnnotationData ( onto, valUri, uri ( "atlas", "propertyValue" ), valueLabel );			

			entityManager = Resources.getInstance ().getEntityManagerFactory ().createEntityManager ();
			AnnotatorAccessor annAccessor = new AnnotatorAccessor ( entityManager );
			
			// Now let's map the numerical value
			mapDataItem ( annAccessor, valUri, pval );
			
			// and the unit
			if ( unit != null ) mapUnit ( annAccessor, valUri, unit );
			
			// Bottom line: it's an experimental factor, report this anyway (possibly add discovered terms, below)
			assertIndividual ( onto, valUri, uri ( "efo", "EFO_0000001" ) );
			
			// And has these labels for the type
			assertData ( onto, valUri, uri ( "dc", "type" ), typeLabel );
			assertAnnotationData ( onto, valUri, uri ( "atlas", "propertyType" ), typeLabel );
			
			
			if ( OLD_MODEL_SUPPORT_FLAG )
			{
				// In the old model, it has a type defined as per the original data (plus, possibly an additional type for 
				// each discovered.
				// So, define a type URI that is specific to this type and a generic subclass of efo:experimental factor
				String typeUri = uri ( "biosd", "exp-prop-type/" + parentAcc + "#" + pvalHash );

				assertLink ( onto, valUri, uri ( "biosd-terms", "has-bio-characteristic-type" ), typeUri );
				assertIndividual ( onto, typeUri, uri ( "efo", "EFO_0000001" ) ); // Experimental factor
				assertAnnotationData ( onto, typeUri, uri ( "rdfs", "label" ), typeLabel );
				assertAnnotationData ( onto, typeUri, uri ( "dc-terms", "title" ), typeLabel );
			}

			// Now, see if the feature annotator has something more to say
			List<AbstractOntoTermAnnotation> pvanns = annAccessor.getAllOntoAnns ( pval );
			for ( AbstractOntoTermAnnotation pvann: pvanns )
			{
				String discoveredTypeUri = pvann.getOntoTermUri ();
				assertIndividual ( onto, valUri, discoveredTypeUri );
				
				// Let's track provenance too
				//
				String dtermProv = pvann.getType ();
				if ( contains ( dtermProv, "ZOOMA" ) ) 
					dtermProv = "ZOOMA";
				else if ( OntoResolverAndAnnotator.ANNOTATION_TYPE_MARKER.equals ( dtermProv ) ) 
					dtermProv = "submitter-provided";
				
				if ( dtermProv != null )
				{
					String annUri = uri ( "biosd", "pvalanntracking#" + hashUriSignature ( valUri + discoveredTypeUri + dtermProv ) );
					
					assertIndividual ( onto, annUri, uri ( "biosd-terms", "SampleAttributeOntologyAnnotation" ) );
					assertData ( onto, annUri, uri ( "dc", "creator"), dtermProv );
					assertLink ( onto, annUri, uri ( "oac", "hasTarget" ), valUri );
					assertLink ( onto, annUri, uri ( "oac", "hasBody" ), discoveredTypeUri );
					
					Double dtermScore = pvann.getScore ();
					// Usually it's non-null, but just in case
					if ( dtermScore != null )
						assertData ( 
							onto, 
							annUri, 
							uri ( "biosd-terms", "has-percent-score"), 
							String.valueOf ( dtermScore ), 
							XSDVocabulary.DOUBLE.toString () 
					);
					
				  // And now the mappings coming from Bioportal Mapping Service
					// 
					mapTextMappings ( discoveredTypeUri );
					
				} // dtermProv != null
				
				if ( OLD_MODEL_SUPPORT_FLAG )
				{
					// In the old model, we need has-type [ a discovered-type ] for each incoming onto term.
					String typeUri1 = uri ( "biosd", "exp-prop-type/ann-based-concept#" + hashUriSignature ( discoveredTypeUri ) );
					assertLink ( onto, valUri, uri ( "biosd-terms", "has-bio-characteristic-type" ), typeUri1 );
					assertIndividual ( onto, typeUri1, discoveredTypeUri );
				}
				
			} // for discovered terms
			
			// Link the prop value to the sample, choosing the proper linking property
			//
			String smpUri = mapFact.getUri ( sample, params );
			if ( pval instanceof BioCharacteristicValue )
				// a direct, more specific sub-property of has-sample-attribute
				assertLink ( onto, smpUri, uri ( "biosd-terms", "has-bio-characteristic" ), valUri );				
			else
			{
				if ( OLD_MODEL_SUPPORT_FLAG )
					// This is 'is about' and it's pretty wrong in the case of samples, since it links an information content 
					// entity to an independent continuant. We're keeping it, but it's deprecated now 
					assertLink ( onto, mapFact.getUri ( sample, params ), uri ( "sio", "SIO_000332" ), valUri );
			}
			
			// sub-property of sio:SIO_000008 ('has attribute'), we define it redundantly, as a super-property, grouping 
			// specific cases.
			assertLink ( onto, mapFact.getUri ( sample, params ), uri ( "biosd-terms", "has-sample-attribute" ), valUri );

			// Now we have has-sample-attribute and possibly more specific properties too
			
			// Woo! We're done!
			return true;
			
		}	// try	
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Error while mapping sample '%s'.[%s]=[%s]: '%s'", 
				sample.getAcc (), pval.getType (), pval, ex.getMessage () ), 
			ex );
		}
		finally {
			if ( entityManager != null && entityManager.isOpen () ) entityManager.close ();
		}
	} // map ()
	
	
	/**
	 * If the property has a numerical value associated (including ranges and dates), this will spawn corresponding 
	 * statements. Numerical values are computed and stored by the BioSD feature annotator.
	 */
	@SuppressWarnings ( "deprecation" )
	private boolean mapDataItem ( AnnotatorAccessor annotatorAccessor, String pvalUri, ExperimentalPropertyValue pv )
	{
		DataItem dataItem = annotatorAccessor.getExpPropValDataItem ( pv );
		if ( dataItem == null ) return false;
		
		RdfMapperFactory mapFact = this.getMapperFactory ();
		OWLOntology onto = mapFact.getKnowledgeBase ();
		
		// Is it a single number? 
		//
		if ( dataItem instanceof NumberItem ) 
		{
			assertData ( onto, 
				pvalUri, 
				uri ( "sio", "SIO_000300") , // has value 
				String.valueOf ( ((NumberItem) dataItem).getValue () ), 
				XSDVocabulary.DOUBLE.toString () 
			);

			return true;
		}
			
		// Or a single date? 
		// 
		if ( dataItem instanceof DateItem ) 
		{
			Date date = ( (DateItem) dataItem ).getValue ();

			if ( date.getHours () == 0 && date.getMinutes () == 0 && date.getSeconds () == 0 )
				// It's a date only
				assertData ( 
					onto, 
					pvalUri, 
					uri ( "sio", "SIO_000300"), 
					DataTypeAdapter.printDate ( date ), 
					XSDVocabulary.DATE.toString () 
			);
			else
				// has th time too
				assertData ( 
					onto, 
					pvalUri, uri ( "sio", "SIO_000300") , 
					DataTypeAdapter.printDateTime ( date ), 
					XSDVocabulary.DATE_TIME.toString () 
			);
				
			return true;
		}
		
		// Interval, then? 
		// 
		if ( dataItem instanceof NumberRangeItem )
		{
			NumberRangeItem range = (NumberRangeItem) dataItem;
			assertIndividual ( onto, pvalUri, uri ( "sio", "SIO_000944" ) ); // interval
			assertData ( 
				onto, 
				pvalUri, uri ( "biosd-terms", "has-low-value" ), String.valueOf ( range.getLow () ),
				XSDVocabulary.DOUBLE.toString () 
			);
			assertData ( 
				onto, 
				pvalUri, 
				uri ( "biosd-terms", "has-high-value" ), 
				String.valueOf ( range.getHi () ), 
				XSDVocabulary.DOUBLE.toString () 
			);
			return true;
		}
		
		throw new IllegalArgumentException ( "Don't know how to map the data item " + dataItem.toString () );
		
	} // mapDataItem ()
	
	
	/**
	 * Exploits a possibly existing ontology term for the unit, either provided by the submitter, or computed by the
	 * feature annotator.
	 */
	private void mapUnit ( AnnotatorAccessor annotatorAccessor, String pvalUri, Unit unit )
	{
		if ( unit == null ) return;
		String unitLabel = StringUtils.trimToNull ( unit.getTermText () );
		if ( unitLabel == null ) return;
		
		// Say something about the unit, if you've its ontology term
		//
		OntologyEntry unitOnto = annotatorAccessor.getUnitOntologyEntry ( unit );
		if ( unitOnto == null ) return; 
		
		String ontoUri = unitOnto.getAcc ();
		
		if ( ! ( startsWith ( ontoUri, "http://" ) || startsWith ( ontoUri, "https://" ) ) )
			return;
		
		RdfMapperFactory mapFact = this.getMapperFactory ();
		OWLOntology onto = mapFact.getKnowledgeBase ();
		
		// As usually, we have a (recyclable) unit individual, which is an instance of a unit class.
		// I'd like to use unitClassUri + prefix for this, but it's recommended not to use other's namespaces
		// TODO: experiment OWL2 punning?
		//
		String unitInstUri = uri ( "biosd", "unit#" + Java2RdfUtils.hashUriSignature ( ontoUri ) );
		assertLink ( onto, pvalUri, uri ( "sio", "SIO_000221" ), unitInstUri ); // 'has unit'
		assertIndividual ( onto, unitInstUri, ontoUri );
		assertAnnotationData ( onto, unitInstUri, uri ( "rdfs", "label" ), unitLabel ); // let's report user details somewhere
		assertAnnotationData ( onto, unitInstUri, uri ( "dc-terms", "title" ), unitLabel ); 
		
		// This is 'unit of measurement'. this is implied by the range of 'has unit', but let's report it, for sake of 
		// completeness and to ease things when no inference is computed
		assertIndividual ( onto, unitInstUri, uri ( "sio", "SIO_000074" ) );
	
	} // mapUnit ()


	/**
	 * This is experimental and not currently working/used.
	 * Fetches the mappings that an ontology term has, using the Bioportal mapping service. Generates corresponding 
	 * triples.
	 */
	@SuppressWarnings ( "unused" )
	private void mapTextMappings ( String ontoTermUri )
	{
		if ( !this.fetchOntologyMappings ) return;
		
		if ( true ) throw new UnsupportedOperationException ( 
			"This version of biosd2rdf no longer supports the Bioportal ontology mapping service, we need to upgrade some code" 
		);
		
		List<OntologyClassMapping> ontoMaps = null;
		String BIOPORTAL_ONTOLOGIES = "TODO: FIX ME!";
		
		// TODO: Fix it!
		BioportalClient bpcli = null; // = otermResolver.getOntologyService ();
		synchronized ( bpcli )
		{
			OntologyClass bpClass = bpcli.getOntologyClass ( null, ontoTermUri );
			//OntologyClass bpClass = null;
			if ( bpClass != null ) 
				ontoMaps = bpcli.getOntologyClassMappings ( bpClass, BIOPORTAL_ONTOLOGIES, false );
		}
		
		if ( ontoMaps == null ) return;

		RdfMapperFactory mapFact = this.getMapperFactory ();
		OWLOntology onto = mapFact.getKnowledgeBase ();

		for ( OntologyClassMapping ontoMap: ontoMaps )
		{
			String targetUri = ontoMap.getTargetClassRef ().getClassIri ();
			if ( ontoTermUri.equals ( targetUri ) ) continue;

			String mapSrc = StringUtils.trimToNull ( ontoMap.getSource () );
			String matchUri = "LOOM,UMLS,REST".contains ( mapSrc ) ? "close"
				: "SAME_URI".equals ( mapSrc ) ? null 
				: "related";
			
			if ( matchUri == null ) continue;
				
			matchUri = uri ( "skos", matchUri + "Match" );

			assertLink ( onto, ontoTermUri, matchUri, targetUri );
			
			// The provenance too!
			String provStr = "Bioportal Mapping Service" +  ( mapSrc == null ? "" : " (" + mapSrc + " source)" );
			String mapAnnUri = uri ( 
				"biosd", 
				"mapanntracking#" + hashUriSignature ( ontoTermUri + targetUri + provStr ) 
			);
			
			assertIndividual ( onto, mapAnnUri, uri ( "biosd-terms", "OntologyMappingAnnotation" ) );
			assertData ( onto, mapAnnUri, uri ( "dc", "creator"), provStr );
			assertLink ( onto, mapAnnUri, uri ( "oac", "hasTarget" ), ontoTermUri );
			assertLink ( onto, mapAnnUri, uri ( "oac", "hasBody" ), targetUri );
			
		} // for each mapping
	
	} // mapTextMappings ()
	
}
