package uk.ac.ebi.fg.java2rdf.mappers;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * It works like {@link ToObjectPropRdfMapper}, but for inverse links, i.e., instead of triples 
 * of type (T, {@link #getTargetPropertyUri()}, PT), it builds triples of type (PT, {@link #getTargetPropertyUri()}, T)
 * for the value found in {@link #getSourcePropertyName()}.
 * 
 * TODO: data properties need a corresponding mapper.
 *
 * <dl><dt>date</dt><dd>25 Jun 2013</dd></dl>
 * @author Marco Brandizi
 */
public class ToObjectInversePropRdfMapper<T, PT> extends ToObjectPropRdfMapper<T, PT>
{
	public ToObjectInversePropRdfMapper ()  {
		super ();
	}

	public ToObjectInversePropRdfMapper ( String sourcePropertyName, String targetPropertyUri ) {
		super ( sourcePropertyName, targetPropertyUri );
	}
	
	
	/**
	 * Generates a triple where the property {@link #getSourcePropertyName()} is asserted for the source, using
	 * {@link #getTargetPropertyUri()}. Uses {@link BeanRdfMapperFactory#getUri(Object)} for both the source and the target URI. 
	 */
	@Override
	public boolean map ( T source, PT propValue )
	{
		try
		{
			if ( propValue == null ) return false;
			BeanRdfMapperFactory mapFactory = this.getMapperFactory ();
			
			String propUri = mapFactory.getUri ( propValue );
			if ( propUri == null ) return false;
			
			OwlApiUtils.assertLink ( this.getMapperFactory ().getKnowledgeBase (), 
				propUri, this.getTargetPropertyUri (), mapFactory.getUri ( source ) );

			// Don't use targetMapper, we need to trace this visit.
			return mapFactory.map ( propValue );
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Error while mapping %s[%s].'%s'[%s] to RDF: %s", 
					propValue.getClass ().getSimpleName (), 
					StringUtils.abbreviate ( propValue.toString (), 50 ), 
					this.getSourcePropertyName (),
					StringUtils.abbreviate ( source.toString (), 50 ), 
					ex.getMessage ()
			));
		}
	}
	
}
