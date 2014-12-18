package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.fgpt.zooma.model.AnnotationSummary;
import uk.ac.ebi.fgpt.zooma.model.Property;
import uk.ac.ebi.fgpt.zooma.model.TypedProperty;
import uk.ac.ebi.fgpt.zooma.search.ZOOMASearchClient;
import uk.ac.ebi.fgpt.zooma.search.ontodiscover.ZoomaOntoTermDiscoverer;

/**
 * 
 * Tests for {@link BioSDCachedOntoTermDiscoverer}, our special cache handler for ontology term discovery.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>15 Dec 2014</dd>
 *
 */
public class BioSDCachedDiscovererTest
{
	/**
	 * Used by {@testNonOntologyTypes #()}
	 *	 
	 */
	private static class MyZOOMASearchClient extends ZOOMASearchClient
	{
		@Override
		public Map<AnnotationSummary, Float> searchZOOMA ( Property property,
				float score, boolean excludeType, boolean noEmptyResult )
		{
			Assert.assertFalse ( "ZOOMA was called with a type!", !excludeType || property instanceof TypedProperty );
			return super.searchZOOMA ( property, score, excludeType, noEmptyResult );
		}
		
	}
	
	/**
	 * Checks that non-ontology terms like 'sample id' are actually skipped.
   *
	 */
	@Test
	public void testNonOntologyTypes ()
	{
		// Assertion inside MyZOOMASearchClient
		BioSDCachedOntoTermDiscoverer discoverer = new BioSDCachedOntoTermDiscoverer (	new ZoomaOntoTermDiscoverer ( new MyZOOMASearchClient () ) );
		discoverer.getOntologyTermUris ( "123", "sample_title" );
		discoverer.getOntologyTermUris ( "ABC", "Synonym" );
	}
}
