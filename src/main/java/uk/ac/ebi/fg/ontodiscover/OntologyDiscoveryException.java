package uk.ac.ebi.fg.ontodiscover;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>May 27, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class OntologyDiscoveryException extends RuntimeException
{
	private static final long serialVersionUID = 5698169216430497619L;

	public OntologyDiscoveryException ( String message ) {
		super ( message );
	}

	public OntologyDiscoveryException ( String message, Throwable cause ) {
		super ( message, cause );
	}

}
