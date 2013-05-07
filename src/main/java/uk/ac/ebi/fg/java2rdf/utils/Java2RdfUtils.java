package uk.ac.ebi.fg.java2rdf.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>May 6, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class Java2RdfUtils
{
	private static MessageDigest messageDigest = null;

	private Java2RdfUtils () {}

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
