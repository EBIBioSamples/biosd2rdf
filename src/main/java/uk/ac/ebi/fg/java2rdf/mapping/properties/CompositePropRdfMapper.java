package uk.ac.ebi.fg.java2rdf.mapping.properties;

import java.util.Arrays;
import java.util.List;

import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>8 Aug 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class CompositePropRdfMapper<T, PT> extends PropertyRdfMapper<T, PT>
{
	private List<PropertyRdfMapper<T, PT>> propertyMappers;
	
	public CompositePropRdfMapper ()
	{
		this ( null );
	}

	public CompositePropRdfMapper ( String targetPropertyUri )
	{
		this ( targetPropertyUri, null ) ;
	}

	public CompositePropRdfMapper ( String targetPropertyUri, PropertyRdfMapper<T, PT> ... propertyMappers ) 
	{
		super ( targetPropertyUri );
		if ( propertyMappers != null ) this.setPropertyMappers ( Arrays.asList ( propertyMappers ) );
	}

	
	@Override
	public boolean map ( T source, PT propValue )
	{
		if ( propertyMappers == null || propertyMappers.isEmpty () ) return false;
		if ( !super.map ( source, propValue ) ) return false;
		
		boolean result = false;
		for ( PropertyRdfMapper<T, PT> mapper: this.propertyMappers )
			result |= mapper.map ( source, propValue );

		return result;
	}

	public List<PropertyRdfMapper<T, PT>> getPropertyMappers ()
	{
		return propertyMappers;
	}

	public void setPropertyMappers ( List<PropertyRdfMapper<T, PT>> propertyMappers )
	{
		this.propertyMappers = propertyMappers;
	}

	@Override
	public void setMapperFactory ( RdfMapperFactory mapperFactory )
	{
		super.setMapperFactory ( mapperFactory );
		if ( this.propertyMappers == null ) return;
		
		for ( PropertyRdfMapper<T, PT> pmapper: this.propertyMappers )
			pmapper.setMapperFactory ( mapperFactory );
	}
	
}
