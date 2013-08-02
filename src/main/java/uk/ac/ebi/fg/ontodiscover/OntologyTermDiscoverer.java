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
	public abstract URI getOntologyTermUri ( String valueLabel, String typeLabel ) throws OntologyDiscoveryException;
	
	public String getOntologyTermUriAsASCII ( String valueLabel, String typeLabel ) throws OntologyDiscoveryException
	{
		URI result = getOntologyTermUri ( valueLabel, typeLabel );
		return result == null ? null : result.toASCIIString ();
	}
}
