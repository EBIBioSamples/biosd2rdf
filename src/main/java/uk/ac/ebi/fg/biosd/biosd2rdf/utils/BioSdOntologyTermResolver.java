package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.core_model.terms.FreeTextTerm;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;
import uk.ac.ebi.fg.ontodiscover.CachedOntoTermDiscoverer;
import uk.ac.ebi.fg.ontodiscover.OntologyTermDiscoverer;
import uk.ac.ebi.fg.ontodiscover.Zooma1OntoTermDiscoverer;
import uk.ac.ebi.ontocat.Ontology;
import uk.ac.ebi.ontocat.OntologyService;
import uk.ac.ebi.ontocat.OntologyServiceException;
import uk.ac.ebi.ontocat.OntologyTerm;
import uk.ac.ebi.ontocat.bioportal.BioportalOntologyService;
import uk.ac.ebi.utils.memory.SimpleCache;

/**
 * Uses a combination of {@link OntologyService} (OLS at the moment) and {@link OntologyTermDiscoverer} (Zooma at the 
 * moment) to associate a BioSD {@link FreeTextTerm} to an ontology URI. It first tries to extract an 
 * {@link OntologyEntry} from the term's associated entries, then it tries to get the URI via the ontology service, if 
 * this doesn't succeed, it falls back to discovering via {@link FreeTextTerm#getTermText()}.
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
		new CachedOntoTermDiscoverer ( new Zooma1OntoTermDiscoverer (), (int) 500E3 );
	private BioportalOntologyService ontologyService;
	private Map<String, String> bioPortalOntologies;
	
	private final SimpleCache<String, String> ontoCache = new SimpleCache<> ( (int) 500E3 ); 

	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public String getOntologyUri ( FreeTextTerm text ) 
	{
		if ( text == null ) return null;
		
		Set<OntologyEntry> oes = text.getOntologyTerms ();
		if ( oes.size () != 1 )
			return ontoTermDiscoverer.getOntologyTermUriAsASCII ( text.getTermText () );
		
		OntologyEntry oe = oes.iterator ().next ();
		String oeAcc = StringUtils.trimToNull ( oe.getAcc () );
		if ( oeAcc == null ) return ontoTermDiscoverer.getOntologyTermUriAsASCII ( text.getTermText () );
		ReferenceSource src = oe.getSource ();
		if ( src == null ) return ontoTermDiscoverer.getOntologyTermUriAsASCII ( text.getTermText () );
		String srcAcc = StringUtils.trimToNull ( src.getAcc () );
		if ( srcAcc == null ) return ontoTermDiscoverer.getOntologyTermUriAsASCII ( text.getTermText () );
		
		// Normalize the accession into a format that can be adapted to the onto-service
		int idx = oeAcc.lastIndexOf ( ':' );
		if ( idx == -1 ) idx = oeAcc.lastIndexOf ( '_' );
		if ( idx != -1 ) oeAcc = oeAcc.substring ( idx + 1 );
		
		oeAcc = oeAcc.toUpperCase (); srcAcc = srcAcc.toUpperCase ();

		String oeCacheKey = srcAcc + ":" + oeAcc;
		String uri = ontoCache.get ( oeCacheKey );
		// If this key is known to yield a null OT, then fall back to discovering via label
		if ( uri != null ) {
			if ( CachedOntoTermDiscoverer.NULL_URI.toASCIIString ().equals ( uri ) ) {
				log.trace ( "Cached null URI for " + oeCacheKey + ", falling back to label-based discovering" );
				uri = ontoTermDiscoverer.getOntologyTermUriAsASCII ( text.getTermText () );
			}
			else
				log.trace ( "Returning cached URI '" + uri + "' for '" + oeCacheKey + "'" );
			return uri;
		}
		
		// So, uri was not in the cache (it's still null), we have to ask the ontology service
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
					for ( String bioPortalAcc: new String[] { srcAcc + "_" + oeAcc, srcAcc + ":" + oeAcc, srcAcc.toLowerCase () + "_" + oeAcc, srcAcc.toLowerCase () + ":" + oeAcc, oeAcc } )
					{
						if ( ( oservTerm = this.ontologyService.getTerm ( bioPortalOntoAcc, bioPortalAcc ) ) != null ) break;
				}
			}
		} 
		catch ( OntologyServiceException ex ) {
			log.warn ( "Internal Error about the Ontology Service: " + ex.getMessage () + ", continuing with label-based discovering only", ex );
			oservTerm = null;
		}
		
		if ( oservTerm != null ) uri = oservTerm.getURI ().toASCIIString ();
		
		if ( uri == null ) 
		{
			// No luck, record that this OE yields no result and fall back to discovering via label
			log.trace ( "No good result found for '" + oeCacheKey + "', falling back to label-based discovery and caching a null uri for this key" );
			uri = ontoTermDiscoverer.getOntologyTermUriAsASCII ( text.getTermText () );
			ontoCache.put ( oeCacheKey, CachedOntoTermDiscoverer.NULL_URI.toASCIIString () );
			return uri;
		}

		// Got something, cache 'n return.
		log.trace ( "caching URI '" + uri + "' for the key '" + oeCacheKey + "'" );
		ontoCache.put ( oeCacheKey, uri );
		return uri;
	}
	
	private BioportalOntologyService getOntologService ()
	{
		if ( ontologyService != null ) return ontologyService;
		
		try 
		{
			// return ontologyService = new OlsOntologyService ();
			ontologyService = new BioportalOntologyService ();
			bioPortalOntologies = new HashMap<> ();
			// Bioportal wants the ontology identified by their damn internal ID and it has no mean to achieve that from
			// the symbolic short name, so we need to cache this rubbish
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
}
