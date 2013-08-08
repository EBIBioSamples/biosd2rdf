package uk.ac.ebi.fg.java2rdf.mapping.properties;

import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>8 Aug 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class InversePropRdfMapper<T, PT> extends PropertyRdfMapper<T, PT>
{
	private PropertyRdfMapper<PT, T> inversePropMapper;
	
	public InversePropRdfMapper () {
		this ( null );
	}
	
	public InversePropRdfMapper ( PropertyRdfMapper<PT, T> inversePropMapper )
	{
		super ();
		this.setInversePropMapper ( inversePropMapper );
	}

	public boolean map ( T source, PT propValue ) 
	{
		if ( this.inversePropMapper == null ) return false;
		if ( propValue == null ) return false;
		
		return inversePropMapper.map ( propValue, source );
	}

	public PropertyRdfMapper<PT, T> getInversePropMapper ()
	{
		return inversePropMapper;
	}

	public void setInversePropMapper ( PropertyRdfMapper<PT, T> inversePropMapper )
	{
		this.inversePropMapper = inversePropMapper;
	}

	@Override
	public void setMapperFactory ( RdfMapperFactory mapperFactory )
	{
		super.setMapperFactory ( mapperFactory );
		if ( this.inversePropMapper != null ) inversePropMapper.setMapperFactory ( mapperFactory );
	}
}
