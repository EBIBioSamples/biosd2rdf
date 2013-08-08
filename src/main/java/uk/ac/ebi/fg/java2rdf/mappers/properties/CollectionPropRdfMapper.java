/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers.properties;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.java2rdf.mappers.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;

/**
 * TODO: COMMENT ME AGAIN!!!
 * 
 * This is similar to {@link PropertyRdfMapper}, but it takes care of those JavaBean properties that return collections, 
 * i.e., multi-value properties. It uses an underlining {@link #getPropertyMapper() property mapper} to map each value 
 * of such a property into a RDF/OWL statement, every statement is spawned pretty by calling 
 * {@link PropertyRdfMapper#map(Object, Object)} for the underlining property mapper.
 *
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class CollectionPropRdfMapper<T, PT> extends PropertyRdfMapper<T, Collection<PT>>
{
	private PropertyRdfMapper<T, PT> propertyMapper;
	
	public CollectionPropRdfMapper () {
		this ( null, null );
	}

	public CollectionPropRdfMapper ( String targetPropertyUri ) {
		this ( targetPropertyUri, null );
	}


	public CollectionPropRdfMapper ( String targetPropertyUri, PropertyRdfMapper<T, PT> propertyMapper ) 
	{
		super ( targetPropertyUri );
		this.setPropertyMapper ( propertyMapper );
	}

	/**
	 * For a property that returns a collection, it goes through all the collection values and invokes 
	 * {@link #getPropertyMapper()}.{@link #map(Object, Collection)} for each value.
	 *  
	 */
	@Override
	public boolean map ( T source, Collection<PT> propValues )
	{
		try
		{
			if ( propValues == null || propValues.isEmpty () ) return false; 

			for ( PT pvalue: propValues ) 
				propertyMapper.map ( source, pvalue );
			return true;
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Error while doing the RDF mapping <%s[%s] '%s' [%s]: %s", 
					source.getClass ().getSimpleName (), 
					StringUtils.abbreviate ( source.toString (), 50 ), 
					this.getTargetPropertyUri (),
					StringUtils.abbreviate ( propValues.toString (), 50 ), 
					ex.getMessage ()
			), ex );
		}
	}

	/** The underlining property mapper used to map single values for the property {@link #getSourcePropertyName()} */
	public PropertyRdfMapper<T, PT> getPropertyMapper () {
		return propertyMapper;
	}

	
	public void setPropertyMapper ( PropertyRdfMapper<T, PT> propertyMapper ) 
	{
		if ( this.getTargetPropertyUri () != null ) propertyMapper.setTargetPropertyUri ( this.getTargetPropertyUri () );
		this.propertyMapper = propertyMapper;
	}

	/**
	 * This sets the same factory for {@link #getPropertyMapper()} too, so you should call the latter before this.
	 */
	@Override
	public void setMapperFactory ( RdfMapperFactory mapperFactory )
	{
		super.setMapperFactory ( mapperFactory );
		if ( this.propertyMapper != null ) this.propertyMapper.setMapperFactory ( mapperFactory );
	}
}
