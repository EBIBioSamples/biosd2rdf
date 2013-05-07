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
public abstract class RdfValueGenerator<T>
{
	public abstract String getValue ( T source );
}
