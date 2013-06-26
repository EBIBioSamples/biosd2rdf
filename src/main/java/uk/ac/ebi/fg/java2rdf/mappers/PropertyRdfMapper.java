/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;


/**
 * Property mappers are used to associate a JavaBean property to an RDFS/OWL property. Property mappers are usually
 * invoked by {@link BeanRdfMapper#map(Object)}.
 * 
 * @param <T> the type of source mapped bean which of property is mapped to RDF.
 * @param <PT> the type of source property that is mapped to RDF.
 *
 * TODO: do we need a field like this.specificRDFValueMapper? 
 * 
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
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
	 * Gets the value of the bean property {@link #getSourcePropertyName()} and then create a RDF statement
	 * uses {@link #map(Object, Object)}, which in turn usually yields a property/value statement having source as 
	 * subject and {@link #getTargetPropertyUri()} as RDF/OWL property.
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
	 * <p>Implements a specific way to map the property value of this source.
	 * You should take care of the case propValue == null, while {@link #map(Object)} takes care of the null source.</p>
	 * 
	 * <p>This usually creates a statement having the source as subject (typically uses {@link BeanRdfMapperFactory#getRdfUriGenerator(Object)}
	 * for its URI), {@link #getTargetPropertyUri()} as RDF/OWL property and a value or another URI as object (again, it
	 * usually uses the factory for that).</p>
	 * 
	 * <p>As usually, it returns true when a real addition occurs.</p>
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
