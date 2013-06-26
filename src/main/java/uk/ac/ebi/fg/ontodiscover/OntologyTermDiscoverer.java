package uk.ac.ebi.fg.ontodiscover;

import java.net.URI;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>May 27, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public abstract class OntologyTermDiscoverer
{
	public abstract URI getOntologyTermUri ( String label ) throws OntologyDiscoveryException;
	
	public String getOntologyTermUriAsASCII ( String label ) throws OntologyDiscoveryException
	{
		URI result = getOntologyTermUri ( label );
		return result == null ? null : result.toASCIIString ();
	}
}
