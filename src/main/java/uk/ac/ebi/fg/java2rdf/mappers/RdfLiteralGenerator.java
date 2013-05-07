/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class RdfLiteralGenerator<T> extends RdfValueGenerator<T>
{
	@Override
	public final String getValue ( T source )
	{
		return getLiteral ( source );
	}
	
	public String getLiteral ( T source ) {
		return source == null ? "" : source.toString ();
	}
}
