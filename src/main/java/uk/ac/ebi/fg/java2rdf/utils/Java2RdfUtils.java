package uk.ac.ebi.fg.java2rdf.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;

/**
 * Some stuff useful for the RDF mapping job performed by the Java2RDF pacakge.
 *
 * <dl><dt>date</dt><dd>May 6, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class Java2RdfUtils
{
	private static MessageDigest messageDigest = null;

	private Java2RdfUtils () {}

	/**
	 * Takes a string that is supposed to represent the identifier of a resource and turns it into an opaque compact and 
	 * URI-compatible representation. At the moment it hashes the parameter (via MD5) and converts the hash into lower-case
	 * hexadecimal. 
	 * 
	 */
	public static String hashUriSignature ( String sig ) 
	{
		if ( messageDigest == null ) try {
			messageDigest = MessageDigest.getInstance ( "MD5" );
		} 
		catch ( NoSuchAlgorithmException ex ) {
			throw new RdfMappingException ( "Internal error, cannot get the MD5 digester from the JVM", ex );
		}
	
		String hash = DatatypeConverter.printHexBinary ( messageDigest.digest ( sig.getBytes () ) );
		hash = hash.toLowerCase ();
		
		return hash;
	}
	
}
