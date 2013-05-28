package uk.ac.ebi.fg.ontodiscover;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.utils.memory.SimpleCache;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>May 27, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class CachedOntoTermDiscoverer extends OntologyTermDiscoverer
{
	private final OntologyTermDiscoverer base;
	
	private final Map<String, URI> cache;
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
		
	public final static URI NULL_URI;
	
	static 
	{
		try {
			NULL_URI = new URI ( "" );
		} 
		catch ( URISyntaxException ex ) {
			throw new Error ( "Very unexpected internal error, cannot create an empty URI" );
		}
		
	}
	
	public CachedOntoTermDiscoverer ( OntologyTermDiscoverer base, int cacheCapacity ) throws IllegalArgumentException
	{
		if ( base == null ) throw new IllegalArgumentException ( "A cached ontology term discoverer needs a non-null base" );
		this.base = base;
		cache = new SimpleCache<> ( cacheCapacity ); 
	}
			
	public CachedOntoTermDiscoverer ( OntologyTermDiscoverer base )
	{
		this ( base, (int) 1E6 );
	}
			
	@Override
	public URI getOntologyTermUri ( String label ) throws OntologyDiscoveryException
	{
  	if ( ( label = StringUtils.trimToNull ( label ) ) == null ) return null;
  	label = label.toLowerCase ();
  	
		URI uri = cache.get ( label );
		if ( uri != null )
		{
			if ( NULL_URI.equals ( uri ) ) uri = null;
			if ( log.isTraceEnabled () )
				log.trace ( "Returning cached result '" + ( uri == null ? null : uri.toASCIIString () ) + "' for '" + label + "'" );
			return uri;
		}
		
		uri = base.getOntologyTermUri ( label );
		if ( uri == null ) 
		{
			log.trace ( "Returning and caching null for '" + label + "'" );
			cache.put ( label, NULL_URI );	
			return null;
		}
		
		
		if ( log.isTraceEnabled () )
			log.trace ( "Returning and caching '" + uri.toASCIIString () + "' for '" + label + "'" );
		cache.put ( label, uri );
		return uri;
	}

	public void clearCache () {
		cache.clear ();
	}
}
