/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class ToObjectPropRdfMapper<T, PT> extends PropertyRdfMapper<T, PT>
{
	public ToObjectPropRdfMapper ()  {
		super ();
	}

	public ToObjectPropRdfMapper ( String sourcePropertyName, String targetPropertyUri ) {
		super ( sourcePropertyName, targetPropertyUri );
	}
	
	
	/**
	 * Generates a triple where the property {@link #getSourcePropertyName()} is asserted for the source, using
	 * {@link #getTargetPropertyUri()}. Uses {@link BeanRdfMapperFactory#getUri(Object)} for both the source and the target URI. 
	 */
	@Override
	public boolean map ( T source, PT propValue )
	{
		if ( source == null || propValue == null ) return false;
		try
		{
			BeanRdfMapperFactory mapFactory = this.getMapperFactory ();
			
			OwlApiUtils.assertLink ( this.getMapperFactory ().getKnowledgeBase (), 
					mapFactory.getUri ( source ), this.getTargetPropertyUri (), mapFactory.getUri ( propValue ) );

			// Don't use targetMapper, we need to trace this visit.
			return mapFactory.map ( propValue );
		} 
		catch ( ClassCastException ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Internal error (mapper mismatching), while mapping %s[%s].'%s'[%s] to RDF: %s", 
					source.getClass ().getSimpleName (), 
					StringUtils.abbreviate ( source.toString (), 15 ), 
					this.getSourcePropertyName (),
					StringUtils.abbreviate ( propValue.toString (), 15 ), 
					ex.getMessage ()
			));
		}
	}
	
}
