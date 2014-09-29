package uk.ac.ebi.fg.ontodiscover;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fgpt.zooma.model.AnnotationSummary;
import uk.ac.ebi.fgpt.zooma.model.Property;
import uk.ac.ebi.fgpt.zooma.model.SimpleTypedProperty;
import uk.ac.ebi.fgpt.zooma.model.SimpleUntypedProperty;
import uk.ac.ebi.fgpt.zooma.search.ZOOMASearchClient;

/**
 * Ontology Discoverer based on <a href = 'http://www.ebi.ac.uk/fgpt/zooma/docs/'>ZOOMA2</a>.
 *
 * <dl><dt>date</dt><dd>23 Oct 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class Zooma2OntoTermDiscoverer extends OntologyTermDiscoverer
{
	private ZOOMASearchClient zoomaClient;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public Zooma2OntoTermDiscoverer () 
	{
		try
		{
			zoomaClient = new ZOOMASearchClient ( new URL ( "http://www.ebi.ac.uk/fgpt/zooma" ) );
		} 
		catch ( MalformedURLException ex ) {
			throw new OntologyDiscoveryException ( "Internal error while instantiating Zooma: " + ex.getMessage (), ex );
		}
	}

	/**
	 * Uses the top-ranked result from {@link ZOOMASearchClient}.searchZOOMA(), it sends to it a pair of value and type
	 * label, depending on the fact that the type is null or not.  
	 */
	@Override
	public URI getOntologyTermUri ( String valueLabel, String typeLabel ) throws OntologyDiscoveryException
	{
		try
		{
			if ( (valueLabel = StringUtils.trimToNull ( valueLabel )) == null ) return null;
			typeLabel = StringUtils.trimToNull ( typeLabel );
			
			Property zprop = typeLabel == null 
				? new SimpleUntypedProperty ( valueLabel ) 
				: new SimpleTypedProperty ( typeLabel, valueLabel ); 
			Map<AnnotationSummary, Float> zresult = zoomaClient.searchZOOMA ( zprop, 70, typeLabel == null );
			
			if ( zresult == null || zresult.size () == 0 ) return null;
			AnnotationSummary zsum = zresult.keySet ().iterator ().next ();

			// TODO: the case they're more than one refers to multiple terms used to describe the label, it's rare, but 
			// might be worth to consider it too somehow
			Collection<URI> semTags = zsum.getSemanticTags ();
			if ( semTags == null || semTags.size () != 1 ) return null;

			return semTags.iterator ().next ();
		} 
		catch ( Exception ex )
		{
			log.error ( String.format ( 
				"Error while consulting Zooma for '%s' / '%s': %s returning null", 
				valueLabel, typeLabel, ex.getMessage () ), ex 
			);
			return null;
		}
	}
}
