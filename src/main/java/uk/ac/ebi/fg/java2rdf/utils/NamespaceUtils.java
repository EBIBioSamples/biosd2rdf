/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.semanticweb.owlapi.vocab.Namespaces;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Apr 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class NamespaceUtils
{
	private NamespaceUtils () {}
	
	@SuppressWarnings ( "serial" )
	private static final Map<String, String> NAMESPACES = new HashMap<String, String> () {{
		put ( "biosd", 		"http://rdf.ebi.ac.uk/fg/biosamples/" );
		put ( "dc", 			"http://purl.org/dc/terms/" ); // TODO 
		put ( "obo", 			"http://purl.obolibrary.org/obo/" );
	}}; 
	
	public static String ns ( String prefix ) {
		return NAMESPACES.get ( prefix );
	}
	
	public static String ns ( String prefix, String relativePath ) 
	{
		prefix = StringUtils.trimToNull ( prefix );
		Validate.notNull ( prefix, "Cannot resolve empty namespace prefix" );
		String namespace = NAMESPACES.get ( prefix );
		Validate.notNull ( namespace, "Namespace prefix '" + prefix + "' not found" );
		return namespace + relativePath;
	}
	
	public static Map<String, String> getNamespaces () {
		return Collections.unmodifiableMap ( NAMESPACES ); 
	}

}
