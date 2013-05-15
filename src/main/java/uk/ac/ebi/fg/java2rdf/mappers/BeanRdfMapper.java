/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanMap;
import org.semanticweb.owlapi.vocab.Namespaces;

import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * A bean mapper has the purpose of mapping a Java Bean into RDF. 
 *
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
@SuppressWarnings ( { "unchecked", "rawtypes" } )
public class BeanRdfMapper<T> extends RdfMapper<T>
{
	/**
	 * A map of bean property name -> property mapper used to map such property to RDF. Basically this mean that 
	 * every bean is declared as an instance of {@link #getTargetRdfClassUri() RDFS/OWL class} and that every bean 
	 * property generates a statement having the URI of the target bean as subject and an RDF/OWL property mapped from the
	 * bean property.
	 * 
	 */
	private Map<String, PropertyRdfMapper> propertyMappers;
	private String targetRdfClassUri;

	private RdfUriGenerator<T> rdfUriGenerator;
	
	public BeanRdfMapper () {
		this ( null );
	}

	public BeanRdfMapper ( String rdfClassUri ) {
		this ( rdfClassUri, null, null );
	}
	

	public BeanRdfMapper ( String rdfClassUri, RdfUriGenerator<T> rdfUriGenerator ) {
		this ( rdfClassUri, rdfUriGenerator, null );
	}
	

	public BeanRdfMapper ( String rdfClassUri, RdfUriGenerator<T> rdfUriGenerator, Map<String, PropertyRdfMapper> propertyMappers )
	{
		super ();
		this.setPropertyMappers ( propertyMappers );
		this.setRdfClassUri ( rdfClassUri );
		this.setRdfUriGenerator ( rdfUriGenerator );
	}
	

	/**
	 * 
	 * Creates a set of subject-centric statements, using {@link #getPropertyMappers()}. Uses {@link #getRdfUriGenerator()}
	 * to get the URI of the 'source' parameter, i.e., the bean to be mapped. Uses {@link #getTargetRdfClassUri()} to 
	 * make a rdf:type statement about 'source'.
	 */
	@Override
	public boolean map ( T source )
	{
		if ( source == null ) return false; 
		String uri = getRdfUriGenerator ().getUri ( source );
		if ( uri == null ) return false;
		
		BeanRdfMapperFactory mapFactory = this.getMapperFactory ();
		
		// Generates and rdf:type statement
		String targetRdfClassUri = getTargetRdfClassUri ();
		if ( targetRdfClassUri != null ) OwlApiUtils.assertIndividual ( mapFactory.getKnowledgeBase (), 
				getRdfUriGenerator ().getUri ( source ), targetRdfClassUri );
		// TODO: else WARN

		for ( String pname: propertyMappers.keySet () )
		{
			PropertyRdfMapper<T, ?> pmapper = propertyMappers.get ( pname );
			pmapper.map ( source );
		}
		return true;
	}

	/**
	 * <p>The JavaBean property named liked {@link PropertyRdfMapper#getSourcePropertyName() pmapper.getSourcePropertyName()} 
	 * is mapped to a target RDFS/OWL property by means of {@link PropertyRdfMapper pmapper}.</p>
	 * 
	 * <p>You should call this before {@link #setMapperFactory(BeanRdfMapperFactory)}, since the latter sets the 
	 * same factory for the property mappers as well.</p>
	 */
	public <PT> PropertyRdfMapper<T, PT> setPropertyMapper ( PropertyRdfMapper<T, PT> pmapper ) 
	{
		if ( propertyMappers == null ) propertyMappers = new HashMap<String, PropertyRdfMapper> ();
		return propertyMappers.put ( pmapper.getSourcePropertyName (), pmapper );
	}

	/**
	 * See {@link #setPropertyMapper(PropertyRdfMapper)}. Here the keys are the JavaBean properties to be mapped.
	 */
	public Map<String, PropertyRdfMapper> getPropertyMappers () {
		return propertyMappers;
	}

	/**
	 * See {@link #setPropertyMapper(PropertyRdfMapper)}. Here the keys are the JavaBean properties to be mapped.
	 */
	public void setPropertyMappers ( Map<String, PropertyRdfMapper> propertyMappers ) {
		this.propertyMappers = propertyMappers;
	}
	
	/**
	 * Maps rdf:type for the class managed by this mapper ( usually it is something that corresponds to T )
	 */
	public String getTargetRdfClassUri () {
		return targetRdfClassUri;
	}

	public void setRdfClassUri ( String targetRdfClassUri ) {
		this.targetRdfClassUri = targetRdfClassUri;
	}
	
	/** The generator used in {@link #map(Object)} to make the URI of the source bean that is mapped to RDF. */
	public RdfUriGenerator<T> getRdfUriGenerator () {
		return rdfUriGenerator;
	}

	public void setRdfUriGenerator ( RdfUriGenerator<T> rdfUriGenerator ) {
		this.rdfUriGenerator = rdfUriGenerator;
	}

	/**
	 * Automatically sets the factory of all the {@link #getPropertyMappers()} set so far.
	 */
	@Override
	public void setMapperFactory ( BeanRdfMapperFactory mapperFactory )
	{
		super.setMapperFactory ( mapperFactory );
		for ( PropertyRdfMapper pmapper: getPropertyMappers ().values () )
			pmapper.setMapperFactory ( mapperFactory );
	}
}
