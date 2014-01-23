package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;
import uk.ac.ebi.fg.ontodiscover.CachedOntoTermDiscoverer;
import uk.ac.ebi.fg.ontodiscover.OntologyTermDiscoverer;
import uk.ac.ebi.fg.ontodiscover.Zooma2OntoTermDiscoverer;
import uk.ac.ebi.ontocat.Ontology;
import uk.ac.ebi.ontocat.OntologyServiceException;
import uk.ac.ebi.ontocat.OntologyTerm;
import uk.ac.ebi.ontocat.bioportal.BioportalOntologyService;
import uk.ac.ebi.ontocat.bioportal.xmlbeans.ConceptBean;
import uk.ac.ebi.utils.memory.SimpleCache;
import uk.ac.ebi.utils.regex.RegEx;


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
	private OntologyTermDiscoverer ontoTermDiscoverer = 
		new CachedOntoTermDiscoverer ( new Zooma2OntoTermDiscoverer (), (int) 500E3 );
	private BioportalOntologyService ontologyService;
	private Map<String, String> bioPortalOntologies;
	
	private final SimpleCache<String, String> ontoCache = new SimpleCache<> ( (int) 500E3 );
	private final SimpleCache<String, String> unitCache = new SimpleCache<> ( 1000 );
	private Map<String, String> uoUnits;

	private final static RegEx COMMENT_RE = new RegEx ( "(Comment|Characteristic)\\s*\\[\\s*(.+)\\s*\\]", Pattern.CASE_INSENSITIVE );
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	/**
	 * Tries to get the URI of an OWL class that the {@link ExperimentalPropertyValue} appears to be instance of. 
	 * 
	 * Several things are checked to do that:<ul> 
	 * <li>if proper {@link OntologyEntry ontology entries} are attached to either the value or its 
	 * {@link ExperimentalPropertyType type}, it uses {@link #getOntologyTermURI(Collection, String)} to fetch their
	 * URI and returns that</li> 
	 * <li>else, it uses the property value label, checking first that it doesn't look like a number, a date, or has a unit,
	 * it just uses the isNumberOrDate parameter for this</li>
	 * <li>if not, it uses the value to lookup a URI via Zooma, using an {@link OntologyTermDiscoverer}</li>
	 * <li>if it cannot find anything via the property value, it tries to get something from the type.</li>
	 * </ul>
	 * 
	 * returns the URI found, or null in case it could not get anything sensible. The result is cached.
	 */
	public String getOntoClassUri ( ExperimentalPropertyValue<?> pval, boolean isNumberOrDate ) 
	{
		if ( pval == null ) return null;
		
		// First, see if it has a defined OE, which makes sense
		String uri = getOntologyTermURI ( pval.getOntologyTerms (), pval.getTermText () );
		if ( uri != null ) return uri;
		
		String pvalLabel = pval.getTermText ();
		ExperimentalPropertyType ptype = pval.getType ();
		if ( ptype == null ) return null; 
		
		String pvalTypeLabel = getExpPropTypeLabel ( ptype );
		if ( pvalTypeLabel == null ) return null;
		
		// Next, see if it is a number
		// Try to resolve the type instead of the value
		if ( isNumberOrDate ) return ontoTermDiscoverer.getOntologyTermUriAsASCII ( pvalTypeLabel, null );

		
		// OK, we have a value, no ontology term attached, no URI, no number, no date, try to resolve the value string

		// First use both value and type labels
		uri = ontoTermDiscoverer.getOntologyTermUriAsASCII ( pvalLabel, pvalTypeLabel );
		if ( uri != null ) return uri;
		
		// If failed, try type-only as well, this should allow to catch stuff like 'del(7herc2-mkrn3)13frdni', 'gene symbol'
		return ontoTermDiscoverer.getOntologyTermUriAsASCII ( pvalTypeLabel, null );
	}
	
	
	/**
	 * If oes has only one element and this contains a proper source identifier and accession, fetches the term URI via
	 * {@link #getOntologService() OntoCAT}. The result is cached.
	 *  
	 */
	public String getOntologyTermURI ( Collection<OntologyEntry> oes, String oeLabel )
	{
		if ( oes == null || oes.size () != 1 ) return null;
		
		OntologyEntry oe = oes.iterator ().next ();
		String oeAcc = StringUtils.trimToNull ( oe.getAcc () );
		if ( oeAcc == null ) return ontoTermDiscoverer.getOntologyTermUriAsASCII ( oeLabel == null ? oeLabel : oe.getLabel (), null );
		
		ReferenceSource src = oe.getSource ();
		if ( src == null ) return ontoTermDiscoverer.getOntologyTermUriAsASCII ( oeLabel == null ? oeLabel : oe.getLabel (), null );
		
		String srcAcc = StringUtils.trimToNull ( src.getAcc () );
		if ( srcAcc == null ) return ontoTermDiscoverer.getOntologyTermUriAsASCII ( oeLabel == null ? oeLabel : oe.getLabel (), null );
		
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
			if ( CachedOntoTermDiscoverer.NULL_URI.toASCIIString ().equals ( uri ) ) {
				log.trace ( "Cached null URI for " + oeCacheKey + ", falling back to label-based discovering" );
				return null;
			}
			log.trace ( "Returning cached URI '" + uri + "' for '" + oeCacheKey + "'" );
			return uri;
		}		
		
		OntologyTerm oservTerm = null;
		try
		{
			if ( getOntologService () != null ) 
			{
				//	OLS			oservTerm = this.ontologyService.getTerm ( srcAcc, srcAcc + ":" + oeAcc );
				String bioPortalOntoAcc = bioPortalOntologies.get ( srcAcc );

				// Try out different combinations for the term accession, this is because the end users suck at using proper
				// term accessions and these are the variants they most frequently bother us with.
				if ( bioPortalOntoAcc != null )
					for ( String bioPortalAcc: new String[] { 
						srcAcc + "_" + acc, srcAcc + ":" + acc,
						srcAcc.toLowerCase () + "_" + acc,
						srcAcc.toLowerCase () + ":" + acc, acc })
					{
						synchronized ( this.ontologyService ) {
							if ( ( oservTerm = this.ontologyService.getTerm ( bioPortalOntoAcc, bioPortalAcc ) ) != null ) break;
						}
				}
			}
		} 
		catch ( OntologyServiceException ex ) {
			log.warn ( "Internal Error about the Ontology Service: " + ex.getMessage () + ", continuing with label-based discovering only", ex );
			oservTerm = null;
		}
		
		if ( oservTerm != null )
			uri = oservTerm.getURI ().toASCIIString ();

		if ( uri == null ) 
		{
			// No luck, record that this OE yields no result and fall back to discovering via label
			log.trace ( "No good result found for '" + oeCacheKey + "', falling back to label-based discovery and caching a null uri for this key" );
				ontoCache.put ( oeCacheKey, CachedOntoTermDiscoverer.NULL_URI.toASCIIString () );
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
		String uri = this.unitCache.get ( unitLabel );
		if ( uri != null ) 
		{
			if ( CachedOntoTermDiscoverer.NULL_URI.toASCIIString ().equals ( uri ) ) {
				log.trace ( "Returning a cached null URI for the unit '{}'", unitLabel );
				return null;
			}
			log.trace ( "Returning cached URI {} for '{}'", uri, unitLabel );
			return uri;
		}		
			
		try
		{
			if ( getOntologService () == null ) return null;

			// Search under the tree branching from uo:unit			
			uri = getUnitUris ().get ( unitLabel );

			// The problem with the above is that it doesn't find synonims, so we use this other approach too.
			// In turn, the problem with searchSubtree() is that it sucks when it has more than one result to yield back 
			// (a bug occurs inside OC). That's why we're using both
			//
			if ( uri == null && getOntologService () != null )
			{ 
				String uoBpAcc = this.bioPortalOntologies.get ( "UO" );
				List<OntologyTerm> terms;
				
				synchronized ( this.bioPortalOntologies ) {
					terms = this.ontologyService.searchSubtree ( uoBpAcc, "UO_0000000", unitLabel );
				}
				if ( terms != null && !terms.isEmpty () )
					uri = terms.iterator ().next ().getURI ().toASCIIString ();
			}
		} 
		catch ( OntologyServiceException ex )
		{
			log.warn ( String.format ( 
				"Internal Error about the Ontology Service: %s, getting nothing for the unit %s", 
				ex.getMessage (), unitLabel ), ex 
			);
		}
		
		log.trace ( "Caching and returning the URI {} for the unit '{}'", uri, unitLabel );
		unitCache.put ( unitLabel, uri == null ? CachedOntoTermDiscoverer.NULL_URI.toASCIIString () : uri );
		
		return uri; 
	}	
	
	private synchronized Map<String, String> getUnitUris () throws OntologyServiceException
	{
		if ( uoUnits != null ) return uoUnits;
		
		uoUnits = new HashMap<> ();

		if ( getOntologService () == null ) return uoUnits;
		String uoBpAcc = this.bioPortalOntologies.get ( "UO" );

		log.info ( "Loading UO units, please wait" );
		Set<OntologyTerm> terms = ontologyService.getAllChildren ( uoBpAcc, "UO_0000000" );
		if ( terms == null || terms.isEmpty () ) throw new OntologyServiceException ( 
			"No units found in UO via OntoCAT/Bioportal"  
		);
		
		for ( OntologyTerm term: terms ) 
		{
			if ( term == null ) {
				log.warn ( "Ignoring null term returned by the Ontology Service" );
				continue;
			}

			String tlabel = StringUtils.trimToNull ( term.getLabel () );
			URI uri = term.getURI ();
			String termAcc = term.getAccession ();
			
			this.cacheNewUOUnit ( tlabel, term.getURI (), termAcc);
			
			// Synonyms?!
			if ( uri == null || !( term instanceof ConceptBean ) ) continue;
			
			for ( String slabel: ((ConceptBean) term).getSynonyms () )
				this.cacheNewUOUnit ( slabel, uri, termAcc );
		}
		
		return this.uoUnits;
	}
	
	private void cacheNewUOUnit ( String unitLabel, URI uri, String termAcc ) throws OntologyServiceException
	{
		if ( unitLabel == null ) {
			log.warn ( "Ignoring term with null label, accession is: {}", termAcc );
			return;
		}
		if ( uri == null ) {
			log.warn ( "Ignoring term '{}' ({}) with no URI", unitLabel, termAcc );
			return;
		}
					
		unitLabel = unitLabel.trim ();			
		if ( this.uoUnits.containsKey ( unitLabel ) ) throw new OntologyServiceException ( 
		  "For some reason UO contains two unit terms with the same label '" + unitLabel + "'"
		);
		log.trace ( "Saving '{}', {}", unitLabel, uri );
		this.uoUnits.put ( unitLabel, uri.toASCIIString () );
	}
	
	/**
	 * OntoCat interface, used in this class for ontology term searches.
	 */
	private synchronized BioportalOntologyService getOntologService ()
	{		
		if ( ontologyService != null ) return ontologyService;
	
		try 
		{
			// return ontologyService = new OlsOntologyService ();
			ontologyService = new BioportalOntologyService ();
			bioPortalOntologies = new HashMap<> ();
			// Bioportal wants the ontology identified by their damn internal ID and it has no mean to achieve that from
			// the symbolic short name, so we need to cache this rubbish
			log.info ( "Loading Bioportal Ontologies, please wait" );
			for ( Ontology onto: ontologyService.getOntologies () )
				bioPortalOntologies.put ( onto.getAbbreviation (), onto.getOntologyAccession () );
			return ontologyService;
		} 
		catch ( OntologyServiceException ex )
		{
			log.warn ( 
				"Internal Error about the Ontology Service: " + ex.getMessage () + ", continuing with label-based discovering only", ex );
			bioPortalOntologies = null;
			return ontologyService = null;
		}
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
}
