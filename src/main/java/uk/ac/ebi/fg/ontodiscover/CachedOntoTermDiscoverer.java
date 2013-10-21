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
			throw new Error ( "Very unexpected internal error, cannot create an empty URI", ex );
		}
		
	}
	
	public CachedOntoTermDiscoverer ( OntologyTermDiscoverer base, int cacheCapacity ) throws IllegalArgumentException
	{
		if ( base == null ) throw new IllegalArgumentException ( 
			"A cached ontology term discoverer needs a non-null base discoverer" 
		);
		this.base = base;
		cache = new SimpleCache<> ( cacheCapacity );
	}
			
	public CachedOntoTermDiscoverer ( OntologyTermDiscoverer base )
	{
		this ( base, (int) 1E6 );
	}
			
	@Override
	public URI getOntologyTermUri ( String valueLabel, String typeLabel ) throws OntologyDiscoveryException
	{
  	if ( ( valueLabel = StringUtils.trimToNull ( valueLabel ) ) == null ) return null;
  	valueLabel = valueLabel.toLowerCase ();
  	
  	if ( ( typeLabel = StringUtils.trimToEmpty ( typeLabel ) ).isEmpty () ) return null;
  	typeLabel = typeLabel.toLowerCase ();
  	
  	// The class name is added to further minimise the small chance that someone synchronise on this same entry 
  	String cacheEntry = this.getClass ().getName() + ":" + valueLabel + ":" + typeLabel;

  	synchronized ( cacheEntry.intern () )
  	{
			URI uri = cache.get ( cacheEntry );
			if ( uri != null )
			{
				if ( NULL_URI.equals ( uri ) ) uri = null;
				if ( log.isTraceEnabled () )
					log.trace ( "Returning cached result '" + ( uri == null ? null : uri.toASCIIString () ) + "' for '" + cacheEntry + "'" );
				return uri;
			}
			
			uri = base.getOntologyTermUri ( valueLabel, typeLabel );
			if ( uri == null ) 
			{
				log.trace ( "Returning and caching null for '" + cacheEntry + "'" );
				cache.put ( cacheEntry, NULL_URI );	
				return null;
			}
		
			if ( log.isTraceEnabled () )
				log.trace ( "Returning and caching '" + uri.toASCIIString () + "' for '" + cacheEntry + "'" );
			cache.put ( cacheEntry, uri );
			return uri;
  	}
	}

	public void clearCache () {
		cache.clear ();
	}
}
