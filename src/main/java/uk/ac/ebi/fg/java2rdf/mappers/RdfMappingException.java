/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Mar 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class RdfMappingException extends RuntimeException
{
	private static final long serialVersionUID = 8073811171331441797L;

	public RdfMappingException ( String message, Throwable cause ) {
		super ( message, cause );
	}

	public RdfMappingException ( String message ) {
		super ( message );
	}
}
