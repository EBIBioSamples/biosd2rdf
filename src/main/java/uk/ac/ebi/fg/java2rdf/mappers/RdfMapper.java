/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

/**
 * This is the bare minimum that is expected from each mapper, essentially, a mapper keeps a pointer to the {@link BeanRdfMapperFactory}
 * that uses it and does its job when the method {@link #map(Object)} is invoked. 
 *
 * <dl><dt>date</dt><dd>Mar 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public abstract class RdfMapper<T>
{
	private BeanRdfMapperFactory mapperFactory;

	/**
	 * Does the mapping. Takes the source parameter and produces statements into {@link #getKnowledgeBase()}. Does this by
	 * generating an RDF value for the source, through {@link #getRdfUriGenerator()}. The mapper might need other
	 * mappers, for objects attached to the source, which it gets via {@link #getMapperFactory()}.
	 * 
	 * Avoid to call this method directly, use {@link BeanRdfMapperFactory#map(Object)} instead. This will trace the
	 * objects that are already mapped.
	 * 
	 * TODO: AOP to hijacks this call to a factory call. 
	 * 
	 * @return true if the entity was actually mapped, or false if not, either because it was ignored for some reason (e.g., 
	 * null URI or decision to exclude certain objects from export), or because it is a duplicate. 
	 */
	public abstract boolean map ( T source );

	public BeanRdfMapperFactory getMapperFactory () {
		return mapperFactory;
	}

	public void setMapperFactory ( BeanRdfMapperFactory mapperFactory ) {
		this.mapperFactory = mapperFactory;
	}
	
}
