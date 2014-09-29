package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Fixes certain characters that are not liked by XML parsers and turns them into a more acceptable representation
 * (an hex code at the moment).
 * 
 * TODO: move it to JUtils? 
 * 
 * <dl><dt>date</dt><dd>23 Oct 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class XmlCharFixer extends FilterOutputStream
{
	public XmlCharFixer ( OutputStream out ) {
		super ( out );
	}

	/** Uses the approach described at http://tinyurl.com/oruw4ye */
	@Override
	public void write ( int c ) throws IOException
	{
    if ( c == 0x9 || c == 0xA || c == 0xD || c >= 0x20 && c <= 0xD7FF || c >= 0xE000 && c <= 0xFFFD
         || c >= 0x10000 && c <= 0x10FFFF )
     super.write ( c );
   else
     this.write ( String.format ( "&#%d;", c & 0xFFFFFFFFL ).getBytes () );
	}
}
