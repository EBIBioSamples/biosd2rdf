package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

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
public class XmlCharFixer extends FilterWriter
{
	public XmlCharFixer ( Writer base ) {
		super ( base );
	}

	
	@Override
	public void write ( int c ) throws IOException
	{
		this.write ( Character.toString ( (char) c ) );
	}

	@Override
	public void write ( char[] cbuf, int off, int len ) throws IOException
	{
		int end = off + len;
		for ( int i = off; i < end; i++ )
		{
			char c = cbuf [ i ];
			
	    if ( c >= '\u0000' && c <= '\u0008' || c == '\u000b' || c == '\u000c' || c >= '\u000e' && c <= '\u0019' 
	    		 || c >= '\u001a' && c <= '\u001f' || c == '\ufffe' || c == '\uffff' )
	    	cbuf [ i ] = '?';
		}
		super.write ( cbuf, off, len );
	}

	@Override
	public void write ( String str, int off, int len ) throws IOException
	{
		char buf[] = new char [ len ];
		str.getChars ( off, off + len, buf, 0 );
		this.write ( buf, 0, len );
	}	
}
