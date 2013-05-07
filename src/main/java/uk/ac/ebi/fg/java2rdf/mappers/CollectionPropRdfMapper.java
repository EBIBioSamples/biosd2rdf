/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class CollectionPropRdfMapper<T, PT> extends PropertyRdfMapper<T, Collection<PT>>
{
	private PropertyRdfMapper<T, PT> propertyMapper;
	
	public CollectionPropRdfMapper () {
		this ( null, null, null );
	}

	public CollectionPropRdfMapper ( String sourcePropertyName, String targetPropertyUri ) {
		this ( sourcePropertyName, targetPropertyUri, null );
	}


	public CollectionPropRdfMapper ( String sourcePropertyName, String targetPropertyUri, PropertyRdfMapper<T, PT> propertyMapper ) 
	{
		super ( sourcePropertyName, targetPropertyUri );
		this.setPropertyMapper ( propertyMapper );
	}

	/**
	 * For a property that returns a collection, it goes through all the collection values and invokes the {@link #getPropertyMapper()}
	 * for each value.
	 *  
	 */
	@Override
	public boolean map ( T source, Collection<PT> propValues )
	{
		if ( source == null || propValues == null || propValues.isEmpty () ) return false; 

		for ( PT pvalue: propValues ) 
		{
			try {
				propertyMapper.map ( source, pvalue );
			} 
			catch ( ClassCastException ex )
			{
				throw new RdfMappingException ( String.format ( 
					"Internal error (mapper mismatching), while mapping %s[%s].'%s'[%s] to RDF: %s", 
						source.getClass ().getSimpleName (), 
						StringUtils.abbreviate ( source.toString (), 15 ), 
						this.getSourcePropertyName (),
						StringUtils.abbreviate ( pvalue.toString (), 15 ), 
						ex.getMessage ()
				));
			}
		}
		return true;
	}

	public PropertyRdfMapper<T, PT> getPropertyMapper () {
		return propertyMapper;
	}

	
	public void setPropertyMapper ( PropertyRdfMapper<T, PT> propertyMapper ) 
	{
		if ( this.getSourcePropertyName () != null ) propertyMapper.setSourcePropertyName ( this.getSourcePropertyName () );
		if ( this.getTargetPropertyUri () != null ) propertyMapper.setTargetPropertyUri ( this.getTargetPropertyUri () );
		this.propertyMapper = propertyMapper;
	}

	@Override
	public void setMapperFactory ( BeanRdfMapperFactory mapperFactory )
	{
		super.setMapperFactory ( mapperFactory );
		this.getPropertyMapper ().setMapperFactory ( mapperFactory );
	}
}
