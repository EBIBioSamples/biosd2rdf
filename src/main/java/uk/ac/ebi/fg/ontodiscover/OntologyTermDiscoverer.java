package uk.ac.ebi.fg.ontodiscover;

import java.net.URI;

/**
 * A generic interface for representing a service that is able to find the URI of an OWL class which of a value/type
 * label strings are assumed to represent an instance. Specific implementations use services like Zooma for that.
 *
 * <dl><dt>date</dt><dd>May 27, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public abstract class OntologyTermDiscoverer
{
	/**
	 * Should return null if no sensible URI was found for the parameters (or if the parameters are null or invalid)
	 */
	public abstract URI getOntologyTermUri ( String valueLabel, String typeLabel ) throws OntologyDiscoveryException;
	
	/**
	 * Just turns {@link #getOntologyTermUri(String, String)} into its ASCII representation (or returns null if
	 * the wrapped method does so). 
	 */
	public String getOntologyTermUriAsASCII ( String valueLabel, String typeLabel ) throws OntologyDiscoveryException
	{
		URI result = getOntologyTermUri ( valueLabel, typeLabel );
		return result == null ? null : result.toASCIIString ();
	}
}
