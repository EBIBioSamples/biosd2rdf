package uk.ac.ebi.fg.java2rdf.mappers.properties;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.java2rdf.mappers.ObjRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>8 Aug 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BeanPropRdfMapper<T, PT> extends ObjRdfMapper<T>
{
	private String sourcePropertyName = null;
	private PropertyRdfMapper<T, PT> propertyMapper = null;
	
	
	public BeanPropRdfMapper () {
		this ( null, null );
	}

	public BeanPropRdfMapper ( String sourcePropertyName, PropertyRdfMapper<T, PT> propertyMapper )
	{
		super ();
		this.setSourcePropertyName ( sourcePropertyName );
		this.setPropertyMapper ( propertyMapper );
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
		if ( propertyMapper == null ) return false;

		PT pval = null;
		try
		{
			pval = (PT) PropertyUtils.getSimpleProperty ( source, sourcePropertyName );
			return propertyMapper.map ( source, pval );
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Internal error while mapping %s[%s].'%s'='%s' to RDF: %s", 
				source.getClass ().getSimpleName (), 
				StringUtils.abbreviate ( source.toString (), 50 ), sourcePropertyName, 
				pval == null ? null : StringUtils.abbreviate ( pval.toString(), 50 ),
				ex.getMessage ()
			), ex);
		}
	}
	
	public String getSourcePropertyName ()
	{
		return sourcePropertyName;
	}

	public void setSourcePropertyName ( String sourcePropertyName )
	{
		this.sourcePropertyName = sourcePropertyName;
	}

	public PropertyRdfMapper<T, PT> getPropertyMapper ()
	{
		return propertyMapper;
	}

	public void setPropertyMapper ( PropertyRdfMapper<T, PT> propertyMapper )
	{
		this.propertyMapper = propertyMapper;
	}

	@Override
	public void setMapperFactory ( RdfMapperFactory mapperFactory )
	{
		super.setMapperFactory ( mapperFactory );
		if ( this.propertyMapper != null ) this.propertyMapper.setMapperFactory ( mapperFactory );
	}

	
}
