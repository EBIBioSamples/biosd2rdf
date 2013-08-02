package uk.ac.ebi.fg.ontodiscover;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import uk.ac.ebi.fgpt.zooma.model.AnnotationSummary;
import uk.ac.ebi.fgpt.zooma.model.Property;
import uk.ac.ebi.fgpt.zooma.model.SimpleTypedProperty;
import uk.ac.ebi.fgpt.zooma.model.SimpleUntypedProperty;
import uk.ac.ebi.fgpt.zooma.search.ZOOMASearchClient;

public class Zooma2OntoTermDiscoverer extends OntologyTermDiscoverer
{
	ZOOMASearchClient zoomaClient;
	
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

	@Override
	public URI getOntologyTermUri ( String valueLabel, String typeLabel ) throws OntologyDiscoveryException
	{
		if ( (valueLabel = StringUtils.trimToNull ( valueLabel )) == null ) return null;
		typeLabel = StringUtils.trimToNull ( typeLabel );
		
		Property zprop = typeLabel == null 
			? new SimpleUntypedProperty ( valueLabel ) 
			: new SimpleTypedProperty ( typeLabel, valueLabel ); 
		Map<AnnotationSummary, Float> zresult = zoomaClient.searchZOOMA ( zprop, 250, typeLabel == null );
		
		if ( zresult == null || zresult.size () == 0 ) return null;
	
// TODO: remove, now Zooma returns results in score-decreasing order
//		// Sort results by score
//		// 
//		@SuppressWarnings ( "unchecked" )
//		Map.Entry<AnnotationSummary, Float>[] zresultIdx = new Map.Entry [ zresult.size () ];
//		zresultIdx = zresult.entrySet ().toArray ( zresultIdx );
		
//		Arrays.sort ( zresultIdx, new Comparator<Map.Entry<AnnotationSummary, Float>>() 
//		{
//			@Override
//			public int compare ( Entry<AnnotationSummary, Float> e1, Entry<AnnotationSummary, Float> e2 )
//			{
//				if ( e1 == null ) return e2 == null ? 0 : -1;
//				if ( e2 == null ) return +1;
//				
//				return e1.getValue ().compareTo ( e2.getValue () );
//			}
//		});
//		
//		// Get the best scored. 
//		AnnotationSummary zsummary = zresultIdx [ 0 ].getKey ();
		AnnotationSummary zsummary = zresult.keySet ().iterator ().next ();
		Collection<URI> semTags = zsummary.getSemanticTags ();

		// TODO: the case they're more than one refers to multiple terms used to describe the label, it's rare, but 
		// might be worth to consider it too somehow
		if ( semTags == null || semTags.size () != 1 ) return null;

		return semTags.iterator ().next ();
	}
}
