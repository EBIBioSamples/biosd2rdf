package uk.ac.ebi.fg.java2rdf.mappers;


/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public abstract class RdfUriGenerator<T> extends RdfValueGenerator<T>
{
	@Override
	public final String getValue ( T source )
	{
		return getUri ( source );
	}

	public abstract String getUri ( T source );
}
