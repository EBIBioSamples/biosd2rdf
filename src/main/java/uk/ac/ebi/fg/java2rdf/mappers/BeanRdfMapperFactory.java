/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Mar 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
@SuppressWarnings ( { "rawtypes", "unchecked" } )
public class BeanRdfMapperFactory
{
	private OWLOntology knowledgeBase;
	private Map<Class, BeanRdfMapper> mappers;
	private Set visitedBeans = new HashSet<> ();
	
	public BeanRdfMapperFactory () {
	}
	
	public BeanRdfMapperFactory ( OWLOntology knowledgeBase ) {
		this.knowledgeBase = knowledgeBase;
	}
	

	/**
	 * The default implementation provides a mapper by looking at the class of the source object. 
	 */
	public <T> BeanRdfMapper<T> getMapper ( T source ) {
		return mappers.get ( source.getClass() );
	}

	public <T> BeanRdfMapper setMapper ( Class<T> clazz,  BeanRdfMapper<T> mapper ) 
	{
		Validate.notNull ( clazz, "Internal error: I cannot map a null class to RDF" );
		Validate.notNull ( mapper, "Internal error: I cannot map '" + clazz.getSimpleName () + "' to RDF using a null mapper" );
		
		if ( mappers == null ) mappers = new HashMap<Class, BeanRdfMapper> ();
		mapper.setMapperFactory ( this );
		return (BeanRdfMapper) mappers.put ( clazz, mapper );
	}
	
	public Map<Class, BeanRdfMapper> getMappers () {
		return mappers;
	}

	public void setMappers ( Map<Class, BeanRdfMapper> mappers ) {
		this.mappers = mappers;
	}
	
	public OWLOntology getKnowledgeBase () {
		return knowledgeBase;
	}

	/**
	 * In case it is really new, {@link #reset()} is invoked. 
	 */
	public void setKnowledgeBase ( OWLOntology knowledgeBase ) 
	{
		if ( this.knowledgeBase == knowledgeBase ) return;
		this.knowledgeBase = knowledgeBase;
		this.reset ();
	}

	/** Always call this, which trace already-mapped beans. Never call {@link BeanRdfMapper#map(Object)} directly. 
	 *  TODO: AOP around the source mapper.
	 */
	public <T> boolean map ( T source )
	{
		if ( source == null ) return false; 
		if ( this.visitedBeans.contains ( ( source ) ) ) return false;
		
		BeanRdfMapper<T> mapper = getMapper ( source );
		if ( mapper == null ) throw new RuntimeException ( 
			"Cannot find a mapper for " + source.getClass ().getSimpleName () );
		
		this.visitedBeans.add ( source );
		return mapper.map ( source ); 
	}
	
	public <T> RdfUriGenerator<T> getRdfIriGenerator ( T source ) 
	{
		if ( source == null ) return null; 
		
		BeanRdfMapper<T> mapper = getMapper ( source );
		if ( mapper == null ) return null;
		
		return mapper.getRdfIriGenerator ();
	}

	public <T> String getUri ( T source ) 
	{
		if ( source == null ) return null; 
		
		RdfUriGenerator<T> iriGen = getRdfIriGenerator ( source );
		if ( iriGen == null ) return null;
		
		return iriGen.getUri ( source );
	}

	/**
	 * Marks all the beans as new, so they'll be re-visited by {@link #map(Object)}, like the first time.
	 */
	public void reset () {
		this.visitedBeans.clear ();
	}

}
