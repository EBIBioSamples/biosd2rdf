/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;


/**
 * TODO: Comment me!
 *
 * TODO: do we need a field like this.specificRDFValueMapper? 
 * 
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public abstract class PropertyRdfMapper<T, PT> extends RdfMapper<T>
{
	private String sourcePropertyName, targetPropertyUri;
	
	public PropertyRdfMapper ()
	{
		super ();
	}


	public PropertyRdfMapper ( String sourcePropertyName, String targetPropertyUri )
	{
		super ();
		this.setSourcePropertyName ( sourcePropertyName );
		this.setTargetPropertyUri ( targetPropertyUri );
	}
	

	/**
	 * Uses the bean property name, defined by {@link #sourcePropertyName} and maps it to RDF via {@link #targetPropertyUri}.
	 */
	@Override
	@SuppressWarnings ( "unchecked" )
	public final boolean map ( T source ) throws RdfMappingException
	{
		if ( source == null ) return false; 
		
		try {
			PT pval = (PT) PropertyUtils.getSimpleProperty ( source, sourcePropertyName );
			return map ( source, pval );
		} 
		catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException ex ) {
			throw new RdfMappingException ( String.format ( 
				"Internal error while mapping %s[%s].'%s' to RDF: %s", 
					source.getClass ().getSimpleName (), StringUtils.abbreviate ( source.toString (), 15 ), sourcePropertyName,
					ex.getMessage ()
			));
		}
	}
	
	/**
	 * Implements a specific way to map the property value of this source.
	 * You should take care of the case propValue == null, while {@link #map(Object)} takes care of the null source.
	 * 
	 * As usually, it returns true when a real addition occurs.
	 */
	public abstract boolean map ( T source, PT propValue );

	public String getSourcePropertyName () {
		return sourcePropertyName;
	}

	public void setSourcePropertyName ( String sourcePropertyName ) {
		this.sourcePropertyName = sourcePropertyName;
	}

	public String getTargetPropertyUri () {
		return targetPropertyUri;
	}

	public void setTargetPropertyUri ( String targetPropertyUri ) {
		this.targetPropertyUri = targetPropertyUri;
	}
}
