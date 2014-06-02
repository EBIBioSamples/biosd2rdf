package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertAnnotationData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertIndividual;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertLink;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.cxf.xjc.runtime.DataTypeAdapter;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import uk.ac.ebi.fg.biosd.biosd2rdf.utils.BioSdOntologyTermResolver;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.Unit;
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
	 * A facility to work out composed numerical/date/unit-equipped values and decompose them into RDF describing
	 * their components. See below how this is used.
	 *  
	 */
	private class PropValueComponents
	{
		public Double value = null, lo = null, hi = null;
		public Date date = null;
		public String valueLabel = null;
		public String unitLabel = null;
		public String unitClassUri = null;
		
		public PropValueComponents ( ExperimentalPropertyValue<?> pval )
		{
			Unit u = pval.getUnit ();
			if ( u != null ) 
			{
				this.unitLabel =  StringUtils.trimToNull ( u.getTermText () );
			
				// See if the unit object has an explicit ontology term attached
				this.unitClassUri = otermResolver.getOntologyTermURI ( u.getOntologyTerms (), this.unitLabel );
				
				if ( this.unitClassUri == null )
					// No? Then, try with the unit ontology
					this.unitClassUri = otermResolver.getUnitUri ( this.unitLabel );
			}

			String pvalStr = StringUtils.trimToNull ( pval.getTermText () );
			if ( pvalStr == null ) return;
			
			this.valueLabel = pvalStr;
			if ( this.unitLabel != null ) this.valueLabel += " " + this.unitLabel;
			
			// Start checking a middle separator, to see if it is a range
			String chunks[] = pvalStr.substring ( 0, Math.min ( pvalStr.length (), 300 ) ).split ( "(\\-|\\.\\.|\\, )" );
			
			if ( chunks != null && chunks.length == 2 )
			{
				chunks [ 0 ] = StringUtils.trimToNull ( chunks [ 0 ] );
				chunks [ 1 ] = StringUtils.trimToNull ( chunks [ 1 ] );
				
				// Valid chunks?
				if ( chunks [ 0 ] != null && chunks [ 1 ] != null )
				{
					// Number chunks?
					if ( NumberUtils.isNumber ( chunks [ 0 ] ) && NumberUtils.isNumber ( chunks [ 1 ] ) )
					{
						try {
							this.lo = Double.parseDouble ( chunks [ 0 ] );
							this.hi = Double.parseDouble ( chunks [ 1 ] );
							return;
						} 
						catch ( NumberFormatException nex ) {
							this.lo = this.hi = null;
							// Just ignore all in case of problems
						}
					}
				} // if valid chunks
			} // if there are chunks
			
			// Is it a single number?
			if ( NumberUtils.isNumber ( pvalStr ) ) 
				try {
					this.value = Double.parseDouble ( pvalStr );
					return;
				}
				catch ( NumberFormatException nex ) {
					this.value = null;
					// Just ignore all in case of problems
			}
				
			// Or maybe a single date?
			// TODO: factorise these constants
			try {
				this.date = DateUtils.parseDate ( pvalStr, 
					"dd'/'MM'/'yyyy", "dd'/'MM'/'yyyy HH:mm:ss", "dd'/'MM'/'yyyy HH:mm", 
					"dd'-'MM'-'yyyy", "dd'-'MM'-'yyyy HH:mm:ss", "dd'-'MM'-'yyyy HH:mm",
					"yyyyMMdd", "yyyyMMdd'-'HHmmss", "yyyyMMdd'-'HHmm"  
				);
			}
			catch ( ParseException dex ) {
				// Just ignore all in case of problems
				this.date = null;
			}
		}
		
		/**
		 * Asserts data values from the structure discovered from an experimental property value string, using the BIOSD/RDF
		 * URI for such property value. 
		 */
		public void map ( String pvalUri )
		{
			OWLOntology onto = ExpPropValueRdfMapper.this.getMapperFactory ().getKnowledgeBase ();
			
			assertData ( onto, pvalUri, ns ( "dc-terms", "title" ), this.valueLabel );
			assertData ( onto, pvalUri, ns ( "rdfs", "label" ), this.valueLabel );
			
			// Say something about the unit, if you've one
			//
			if ( this.unitClassUri != null )
			{
				// As usually, we have a (recyclable) unit individual, which is an instance of a unit class.
				// I'd like to use unitClassUri + prefix for this, but it's recommended not to use other's namespaces
				// TODO: experiment OWL2 punning?
				//
				String unitInstUri = ns ( "biosd", "unit#" + Java2RdfUtils.hashUriSignature ( this.unitClassUri ) );
				assertLink ( onto, pvalUri, ns ( "sio", "SIO_000221" ), unitInstUri ); // 'has unit'
				assertIndividual ( onto, unitInstUri, this.unitClassUri );
				assertAnnotationData ( onto, unitInstUri, ns ( "rdfs", "label" ), this.unitLabel ); // let's report user details somewhere
				assertAnnotationData ( onto, unitInstUri, ns ( "dc-terms", "title" ), this.unitLabel ); 
				
				// This is 'unit of measurement'. this is implied by the range of 'has unit', but let's report it, for sake of 
				// completeness and to ease things when no inference is computed
				assertIndividual ( onto, unitInstUri, ns ( "sio", "SIO_000074" ) );
			}
			
			
			// Is it a single number? 
			//
			if ( this.value != null ) {
				// has value
				assertData ( onto, pvalUri, ns ( "sio", "SIO_000300") , String.valueOf ( this.value ), XSDVocabulary.DOUBLE.toString () );
				return;
			}
				
			// Or a single date? 
			// 
			if ( this.date != null ) {
				// has value
				assertData ( onto, pvalUri, ns ( "sio", "SIO_000300") , DataTypeAdapter.printDateTime ( this.date ), XSDVocabulary.DATE_TIME.toString () );
				return;
			}
			
			// Interval, then? 
			// 
			if ( this.lo != null && this.hi != null )
			{
				assertIndividual ( onto, pvalUri, ns ( "sio", "SIO_000944" ) ); // interval
				assertData ( onto, pvalUri, ns ( "biosd-terms", "has-low-value" ), String.valueOf ( this.lo ), XSDVocabulary.DOUBLE.toString () );
				assertData ( onto, pvalUri, ns ( "biosd-terms", "has-high-value" ), String.valueOf ( this.hi ), XSDVocabulary.DOUBLE.toString () );
								
				return;
			}
		}
		
		/**
		 * True if it's has a unit, or a value, or range values, which can be numerical or date values
		 */
		public boolean isNumberOrDate ()
		{
			return 
				this.unitLabel != null || this.value != null || ( this.lo != null && this.hi != null ) || this.date != null; 
		}
	}
	
	private static BioSdOntologyTermResolver otermResolver = new BioSdOntologyTermResolver ();
	
	public ExpPropValueRdfMapper ()
	{
		super ();
	}

	@Override
	public boolean map ( T sample, ExperimentalPropertyValue pval, Map<String, Object> params )
	{
		try
		{
			// TODO: warnings
			if ( pval == null ) return false;
						
			String sampleAcc = StringUtils.trimToNull ( sample.getAcc () );
			if ( sampleAcc == null ) return false;

			PropValueComponents vcomp = new PropValueComponents ( pval );
			if ( vcomp.valueLabel == null ) return false;
			
			// Process the type
			// 
			ExperimentalPropertyType ptype = pval.getType ();
			
			String typeLabel = BioSdOntologyTermResolver.getExpPropTypeLabel ( ptype );
			if ( typeLabel == null ) return false;
			
			// TODO: is this the same as getAcc() or a secondary accession? 
			// We're ignoring it here, cause the accession is already assigned by the sample mapper.
			if ( "sample accession".equalsIgnoreCase ( typeLabel ) ) return false;

			RdfMapperFactory mapFact = this.getMapperFactory ();
			OWLOntology onto = mapFact.getKnowledgeBase ();
			
			String typeLabelLC = typeLabel.toLowerCase ();
			
			// name -> dc:title and similar
			if ( typeLabel != null && typeLabelLC.matches ( "(sample |group |sample group |)?name" ) )
			{
				assertData ( onto, mapFact.getUri ( sample, params ), ns ( "dc-terms", "title" ), vcomp.valueLabel );
				assertData ( onto, mapFact.getUri ( sample, params ), ns ( "rdfs", "label" ), vcomp.valueLabel );
				return true;
			}

			// 'Sample Description' -> dc-terms:description and similar
			if ( typeLabel != null && typeLabelLC.matches ( "(sample |group |sample group |)?description" ) )
			{
				assertData ( onto, mapFact.getUri ( sample, params ), ns ( "dc-terms", "description" ), vcomp.valueLabel );
				assertData ( onto, mapFact.getUri ( sample, params ), ns ( "rdfs", "comment" ), vcomp.valueLabel );
				return true;
			}
			
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
			String pvalHash = hashUriSignature ( typeLabel + vcomp.valueLabel );
			valUri = ns ( "biosd", "exp-prop-val/" + parentAcc + "#" + pvalHash ); 
			
			// Define its label and possible additional stuff, such as the numerical value, unit, range (see above)
			vcomp.map ( valUri );
			
			// Define a type URI that is specific to this type and a generic subclass of efo:experimental factor
			// TODO: this would be more correct, if it didn't cause Zooma to discover that 1964 is an NCBI term and
			// the same property type is both an organism and a year
			//typeUri = ns ( "biosd", "exp-prop-type/" + parentAcc + "#" + hashUriSignature (  typeLabel ) );
			typeUri = ns ( "biosd", "exp-prop-type/" + parentAcc + "#" + pvalHash );
			
			// Bottom line: it's an experimental factor
			assertIndividual ( onto, valUri, ns ( "efo", "EFO_0000001" ) );
			
			// Another basic fact: it has a type defined as per the original data
			assertLink ( onto, valUri, ns ( "biosd-terms", "has-bio-characteristic-type" ), typeUri );
			assertIndividual ( onto, typeUri, ns ( "efo", "EFO_0000001" ) ); // Experimental factor
			assertAnnotationData ( onto, typeUri, ns ( "rdfs", "label" ), typeLabel );
			assertAnnotationData ( onto, typeUri, ns ( "dc-terms", "title" ), typeLabel );

			// Now, see if Zooma has something more to say
			String discoveredTypeUri = otermResolver.getOntoClassUri ( pval, vcomp.isNumberOrDate () );

			// And in case it has, state that as additional types TODO: only one of such type is for the moment available, 
			// it makes sense to take the first ones top-ranked by Zooma. 
			if( discoveredTypeUri != null )
			{
				String typeUri1 = typeUri + ":1";
				assertLink ( onto, valUri, ns ( "biosd-terms", "has-bio-characteristic-type" ), typeUri1 );
				assertIndividual ( onto, typeUri1, discoveredTypeUri );
			}
			
			// Establish how to link the prop value to the sample
			String attributeLinkUri = pval instanceof BioCharacteristicValue  
				? ns ( "biosd-terms", "has-bio-characteristic" ) // sub-property of sio:SIO_000008 ('has attribute') 
				: ns ( "sio", "SIO_000332" );	// is about
				
			// Now we have either *** sample has-biocharacteristic valUri ***, or *** sample is-about valUri ***, depending on 
			// the Java type. As said above, biosd:has-biocharacteristic is a subproperty of iao:is-about anyway.
			assertLink ( onto, mapFact.getUri ( sample, params ), attributeLinkUri, valUri );
			
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
