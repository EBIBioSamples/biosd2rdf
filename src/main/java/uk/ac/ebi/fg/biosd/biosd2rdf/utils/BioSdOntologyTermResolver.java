package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.bioportal.webservice.client.BioportalClient;
import uk.ac.ebi.bioportal.webservice.exceptions.OntologyServiceException;
import uk.ac.ebi.bioportal.webservice.model.OntologyClass;
import uk.ac.ebi.bioportal.webservice.utils.BioportalWebServiceUtils;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;
import uk.ac.ebi.fgpt.zooma.search.StatsZOOMASearchFilter;
import uk.ac.ebi.fgpt.zooma.search.ZOOMASearchClient;
import uk.ac.ebi.fgpt.zooma.search.ontodiscover.ZoomaOntoTermDiscoverer;
import uk.ac.ebi.onto_discovery.api.OntologyTermDiscoverer;
import uk.ac.ebi.onto_discovery.api.OntologyTermDiscoverer.DiscoveredTerm;
import uk.ac.ebi.onto_discovery.bioportal.BioportalOntoTermDiscoverer;
import uk.ac.ebi.utils.regex.RegEx;

import com.google.common.cache.CacheBuilder;


/**
 * 
 * Everything is cached, so you shouldn't worry about the performance of multiple calls, as long as you keep the same 
 * instance of this class between them.
 * 
 * TODO: logging.
 *
 * <dl><dt>date</dt><dd>May 27, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdOntologyTermResolver
{
	public static final String ONTO_DISCOVERER_PROP_NAME = "uk.ac.ebi.fg.biosd.biosd2rdf.ontoDiscoverer";
	
	private static final String BIOPORTAL_API_KEY = "07732278-7854-4c4f-8af1-7a80a1ffc1bb";

	private OntologyTermDiscoverer ontoTermDiscoverer; 
	
	private BioportalClient ontologyService = new BioportalClient ( BIOPORTAL_API_KEY );
	
	@SuppressWarnings ( { "unchecked", "rawtypes" } )
	private final Map<String, String> ontoCache = ( (CacheBuilder) CacheBuilder.newBuilder () 
		.maximumSize ( (long) 500E3 ) ) 
		.build ().asMap (); 
			
	private Map<String, String> uoUnits;

	private final static RegEx COMMENT_RE = new RegEx ( "(Comment|Characteristic)\\s*\\[\\s*(.+)\\s*\\]", Pattern.CASE_INSENSITIVE );
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	static
	{
		BioportalWebServiceUtils.STATS_WRAPPER.setPopUpExceptions ( false );
	}
	
	
	{		
		OntologyTermDiscoverer baseDiscoverer = null;
		
		String ontoDiscovererProp = System.getProperty ( ONTO_DISCOVERER_PROP_NAME, "zooma" );
		
		if ( "zooma".equalsIgnoreCase ( ontoDiscovererProp ) )
		{
			StatsZOOMASearchFilter zoomaSearcher = new StatsZOOMASearchFilter ( new ZOOMASearchClient () );
			baseDiscoverer = new ZoomaOntoTermDiscoverer ( zoomaSearcher );
		}
		else if ( "bioportal".equalsIgnoreCase ( ontoDiscovererProp ) )
		{
			baseDiscoverer = new BioportalOntoTermDiscoverer ( BIOPORTAL_API_KEY );
			((BioportalOntoTermDiscoverer) baseDiscoverer).setPreferredOntologies ( 
				"EFO,UBERON,CL,CHEBI,BTO,GO,OBI,MESH,FMA,IAO,HP,BAO,MA,ICD10CM,NIFSTD,DOID,IDO,LOINC,OMIM,SIO,CLO,FHHO" 
			);
		}
		else throw new IllegalArgumentException ( 
			"Invalid value '" + ontoDiscovererProp + "' for '" + ONTO_DISCOVERER_PROP_NAME + "'" 
		);

		log.info ( "Ontology Discoverer set to '{}'", ontoDiscovererProp );
		this.ontoTermDiscoverer = new BioSDCachedOntoTermDiscoverer (	baseDiscoverer );
	}
	
	
	/**
	 * Tries to get the URI of an OWL class that the {@link ExperimentalPropertyValue} appears to be instance of. 
	 * 
	 * Several things are checked to do that:
	 * <ul> 
	 * <li>if proper {@link OntologyEntry ontology entries} are attached to either the value or its 
	 * {@link ExperimentalPropertyType type}, it uses {@link #getOntologyTermURI(Collection, String)} to fetch their
	 * URI and returns that</li> 
	 * <li>else, it uses the property value label, checking first that it doesn't look like a number, a date, or has a unit,
	 * it just uses the isNumberOrDate parameter for this</li>
	 * <li>if not, it uses the value to lookup a URI via ZOOMA or Bioportal Annotator, using an 
	 * {@link OntologyTermDiscoverer}</li>
	 * <li>if it cannot find anything via the property value, it tries to get something from the type.</li>
	 * </ul>
	 * 
	 * returns the URI found, or null in case it could not get anything sensible. The result is cached.
	 */
	public List<String> getOntoClassUris ( ExperimentalPropertyValue<?> pval, boolean isNumberOrDate ) 
	{
		if ( pval == null ) return new ArrayList<String> ();
		
		// First, see if it has a defined OE 
		String uri = getOntologyTermURI ( pval.getOntologyTerms (), pval.getTermText () );
		if ( uri != null ) return new ArrayList<String> ( Collections.singletonList ( uri ) );
		
		String pvalLabel = pval.getTermText ();
		ExperimentalPropertyType ptype = pval.getType ();
		if ( ptype == null ) return new ArrayList<String> (); 
		
		String pvalTypeLabel = getExpPropTypeLabel ( ptype );
		if ( pvalTypeLabel == null ) return new ArrayList<String> ();
		
		// Next, see if it is a number
		// Try to resolve the type instead of the value
		if ( isNumberOrDate ) {
			// TODO: for the moment we keep the first result, we'd like to add the others too
			return discoveredTerms2Uris ( ontoTermDiscoverer.getOntologyTerms ( null, pvalTypeLabel ) );
		}

		// OK, we have a value, no ontology term attached, no URI, no number, no date, try to resolve the value string 
		// (via onto term discoverer)

		// First use both value and type labels
		List<String> uris = discoveredTerms2Uris ( ontoTermDiscoverer.getOntologyTerms ( pvalLabel, pvalTypeLabel )	);
		if ( !uris.isEmpty () ) return uris;
		
		// If failed, try type-only as well, this should allow to catch stuff like 'del(7herc2-mkrn3)13frdni', 'gene symbol'
		// Note that the ZOOMA/Bioportal clients don't understand val = null, type = x, but it gets the reverse
		return discoveredTerms2Uris ( ontoTermDiscoverer.getOntologyTerms ( pvalTypeLabel, null ) );
	}
	
	
	/**
	 * If oes has only one element and this contains a proper source identifier and accession, fetches the term URI via
	 * Bioportal. The result is cached.
	 *  
	 */
	public String getOntologyTermURI ( Collection<OntologyEntry> oes, String oeLabel )
	{
		// DEBUG
		if ( "true".equals ( System.getProperty ( "biosd2rdf.debug.fast_mode" ) ) ) return null;		

		if ( oes == null || oes.size () != 1 ) return null;
		
		OntologyEntry oe = oes.iterator ().next ();
		String oeAcc = StringUtils.trimToNull ( oe.getAcc () );
		if ( oeAcc == null ) return getFirstDiscoveredUri ( 
			ontoTermDiscoverer.getOntologyTerms ( null, oeLabel != null ? oeLabel : oe.getLabel () )
		);
		
		ReferenceSource src = oe.getSource ();
		if ( src == null ) 
		{ 
			if ( oeAcc.startsWith ( "http://" ) )
				// We trust that this is a term specified by giving the URI straight. 
				// TODO: needs review, like the BioSD model to be changed to explicitly represent this case.
				return oeAcc;
			else
				// Let's do a final attempt using the term label
				return getFirstDiscoveredUri ( 
					ontoTermDiscoverer.getOntologyTerms ( null, oeLabel == null ? oeLabel : oe.getLabel () )
				);
		}
		
		String srcAcc = StringUtils.trimToNull ( src.getAcc () );
		if ( srcAcc == null ) return getFirstDiscoveredUri ( 
			ontoTermDiscoverer.getOntologyTerms ( null, oeLabel == null ? oeLabel : oe.getLabel () )
		);
		
		return this.getOntologyTermURI ( oeAcc, srcAcc );
	}
	
	
	/**
	 *  Tries to resolve an ontology term from its accession and source symbol, using {@link #getOntologService()}.
	 *  null if nothing useful is found. 
	 */
	public String getOntologyTermURI ( String acc, String srcAcc )
	{
		// Normalise the accession into a format that can be adapted to the onto-service
		int idx = acc.lastIndexOf ( ':' );
		if ( idx == -1 ) idx = acc.lastIndexOf ( '_' );
		if ( idx != -1 ) acc = acc.substring ( idx + 1 );
		
		acc = acc.toUpperCase (); srcAcc = srcAcc.toUpperCase ();

		String oeCacheKey = srcAcc + ":" + acc;
		
		String uri = ontoCache.get ( oeCacheKey );
				
		// If this key is known to yield a null OT, then fall back to discovering via label
		if ( uri != null ) {
			if ( "__NULL__".equals ( uri ) ) {
				log.trace ( "Cached null URI for " + oeCacheKey + ", falling back to label-based discovering" );
				return null;
			}
			log.trace ( "Returning cached URI '" + uri + "' for '" + oeCacheKey + "'" );
			return uri;
		}		
		
		OntologyClass oservTerm = null;
		try
		{
				// Try out different combinations for the term accession, this is because the end users suck at using proper
				// term accessions and these are the variants they most frequently bother us with.
				if ( srcAcc != null )
					for ( String testAcc: new String[] { 
						acc, 
						srcAcc + "_" + acc, 
						srcAcc + ":" + acc,
						srcAcc.toLowerCase () + "_" + acc,
						srcAcc.toLowerCase () + ":" + acc })
					{
						synchronized ( this.ontologyService ) {
							if ( ( oservTerm = this.ontologyService.getOntologyClass ( srcAcc, testAcc ) ) != null ) break;
						}
				}
		} 
		catch ( OntologyServiceException ex ) {
			log.warn ( "Internal Error about the Ontology Service: " + ex.getMessage () + ", continuing with label-based discovering only", ex );
			oservTerm = null;
		}
		
		if ( oservTerm != null )
			uri = oservTerm.getIri ();

		if ( uri == null ) 
		{
			// No luck, record that this OE yields no result and fall back to discovering via label
			log.trace ( "No good result found for '" + oeCacheKey + "', falling back to label-based discovery and caching a null uri for this key" );
				ontoCache.put ( oeCacheKey, "__NULL__" );
			return null;
		}

		// Got something, cache 'n return.
		log.trace ( "caching URI '" + uri + "' for the key '" + oeCacheKey + "'" );
		ontoCache.put ( oeCacheKey, uri );
		return uri;
	}
	
	/**
	 * Returns the URI from the Unit Ontology of this unit label. Uses the {@link #getOntologService()} and caches 
	 * the result. Constraints the result within the subtree of UO_0000000 (unit of measurement), so that we 
	 * don't get bad things such as G = the 'giga' prefix.
	 * 
	 */
	public String getUnitUri ( String unitLabel )
	{
		// DEBUG
		if ( "true".equals ( System.getProperty ( "biosd2rdf.debug.fast_mode" ) ) ) return null;		
			
		// Search under the tree branching from uo:unit			
		return getUnitUris ().get ( unitLabel );
	}	
		
	
	
	
	private synchronized Map<String, String> getUnitUris () throws OntologyServiceException
	{
		if ( uoUnits != null ) return uoUnits;
		
		uoUnits = new HashMap<> ();

		log.info ( "Loading UO units, please wait" );
		Set<OntologyClass> terms = this.ontologyService.getClassDescendants ( "UO", "UO_0000000" );
		if ( terms == null || terms.isEmpty () ) throw new OntologyServiceException ( 
			"No units found in UO via OntoCAT/Bioportal"  
		);
		
		for ( OntologyClass term: terms ) 
		{
			if ( term == null ) {
				log.warn ( "Ignoring null term returned by the Ontology Service" );
				continue;
			}

			String tlabel = StringUtils.trimToNull ( term.getPreferredLabel () );
			String uri = term.getIri ();
			
			this.cacheNewUOUnit ( tlabel, uri );
			
			if ( uri == null ) continue;
			
			for ( String slabel: term.getSynonyms () )
				this.cacheNewUOUnit ( slabel, uri );
		}
		
		return this.uoUnits;
	}
	
	private void cacheNewUOUnit ( String unitLabel, String uri ) throws OntologyServiceException
	{
		if ( unitLabel == null ) {
			log.warn ( "Ignoring unit term with null label, URI is: {}", uri );
			return;
		}
		if ( uri == null ) {
			log.warn ( "Ignoring unit term '{}' with no URI", unitLabel );
			return;
		}

		if ( "A".equals ( unitLabel ) && uri.endsWith ( "UO_0000019" ) )
			// This is angstrom and it's represented as an 'A', but the wrong synonim is returned
			unitLabel = "Å";
		else if ( "micrometre".equals ( unitLabel ) && uri.endsWith ( "UO_0000016" ) )
			// The URI is actually about millimeter, which is already mapped
			return;
		
		unitLabel = unitLabel.trim ();
		String storedUri = this.uoUnits.get ( unitLabel );
		
		if ( storedUri != null && !uri.equals ( storedUri ) ) throw new OntologyServiceException ( String.format ( 
		  "For some reason UO contains two URIs for the label '%s': ('%s', '%s')", unitLabel, uri, storedUri
		));
		log.trace ( "Saving '{}', {}", unitLabel, uri );
		this.uoUnits.put ( unitLabel, uri );
	}
	

	/**
	 * Extract the type label, considering things like Comment [ X ] (return 'X' in such cases).
	 */
	public static String getExpPropTypeLabel ( ExperimentalPropertyType ptype ) 
	{
		if ( ptype == null ) return null;
		String typeLabel = StringUtils.trimToNull ( ptype.getTermText () );
		if ( typeLabel == null ) return null;
		
		String[] chunks = COMMENT_RE.groups ( typeLabel );
		if ( chunks == null || chunks.length < 3 ) return typeLabel;
		return chunks [ 2 ];
	}
	
	private static String getFirstDiscoveredUri ( List<DiscoveredTerm> dterms )
	{
		return dterms == null || dterms.isEmpty () ? null : dterms.get ( 0 ).getIri ();
	}
	
	private static List<String> discoveredTerms2Uris ( List<DiscoveredTerm> dterms )
	{
		List<String> result = new ArrayList<String> ();
		if ( dterms == null || dterms.isEmpty () ) return result;
		for ( DiscoveredTerm dterm: dterms )
			result.add ( dterm.getIri () );
		
		return result;
	}

}
