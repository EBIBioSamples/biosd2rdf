/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * This maps the value of a JavaBean property ot an OWL object-proeperty. It uses {@link #getMapperFactory()} and 
 * its method {@link BeanRdfMapperFactory#getRdfUriGenerator(Object)} to get URIs for the source and target beans
 * to be mapped. For example, to map an instance of b of a Java class Book, having b.author = a, with a as an instance of 
 * the Java class Author, to a statement like: http://rdf.example.com/isbn/123 ex:has-author http://example.com/author/asimov, 
 * you'll define an oobject property mapper having {@link #getSourcePropertyName()} = ex:has-author, while the 
 * subject's URI will be provided by the {@link BeanRdfMapper} for Book 
 * (via the method {@link BeanRdfMapper#getRdfUriGenerator()}) and the object's URI will be given by the {@link BeanRdfMapper} 
 * for Author (again, via {@link BeanRdfMapper#getRdfUriGenerator()}. The bean mappers and their URI generators will be 
 * invoked by the {@link #getMapperFactory() mapper factory associated to this property mapper}.  
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
