package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple filter, which removes characters that are not valid within XML 1.0 (and cause parsers to 
 * fail) with a conventional character '?'. This characters often occurs due to charset problems.
 *
 * <dl><dt>date</dt><dd>8 Oct 2014</dd></dl>
 * @author Marco Brandizi
 *
 */
public class XmlCharFixerTest
{	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	@Test
	public void testGamma () throws UnsupportedEncodingException, IOException
	{
		String testString = "alterations and identifies the PPARγ pathway";
		testRewriting ( testString, testString );
	}

	@Test
	public void testWrongGamma () throws UnsupportedEncodingException, IOException
	{
		String testString = "issue-specific alterations and identifies the PPARÃŽÂ³";
		testRewriting ( testString, testString );
	}

	
	@Test
	public void testBadXMLChar () throws UnsupportedEncodingException, IOException
	{
		testRewriting ( "I have a bad char: '" + ((char) 0x15) + "'", "I have a bad char: '?'" );
	}

	private void testRewriting ( String testInput, String expectedOut ) throws UnsupportedEncodingException, IOException
	{
		StringWriter out = new StringWriter ();
		XmlCharFixer xout = new XmlCharFixer ( out );
		xout.write ( testInput );
		String outStr = out.toString ();
		xout.close ();

		log.info ( "Output: " + outStr );
		
		Assert.assertEquals ( "The XML fixing didn't work!", outStr, expectedOut );
	}
}
