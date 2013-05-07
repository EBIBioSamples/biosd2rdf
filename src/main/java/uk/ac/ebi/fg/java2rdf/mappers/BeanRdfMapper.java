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
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
@SuppressWarnings ( { "unchecked", "rawtypes" } )
public class BeanRdfMapper<T> extends RdfMapper<T>
{
	/**
	 * A map of bean property name -> property mapper used to map such property to RDF. 
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
	 * It creates a set of subject-centric statements, using {@link #propertyMappers}.
	 */
	@Override
	public boolean map ( T source )
	{
		if ( source == null ) return false; 
		String uri = getRdfIriGenerator ().getUri ( source );
		if ( uri == null ) return false;
		
		BeanRdfMapperFactory mapFactory = this.getMapperFactory ();
		
		// Generates and rdf:type statement
		String targetRdfClassUri = getTargetRdfClassUri ();
		if ( targetRdfClassUri != null ) OwlApiUtils.assertIndividual ( mapFactory.getKnowledgeBase (), 
				getRdfIriGenerator ().getUri ( source ), targetRdfClassUri );
		// TODO: else WARN

		for ( String pname: propertyMappers.keySet () )
		{
			PropertyRdfMapper<T, ?> pmapper = propertyMappers.get ( pname );
			pmapper.map ( source );
		}
		return true;
	}
	
	public <PT> PropertyRdfMapper<T, PT> setPropertyMapper ( PropertyRdfMapper<T, PT> pmapper ) 
	{
		if ( propertyMappers == null ) propertyMappers = new HashMap<String, PropertyRdfMapper> ();
		return propertyMappers.put ( pmapper.getSourcePropertyName (), pmapper );
	}

	public Map<String, PropertyRdfMapper> getPropertyMappers () {
		return propertyMappers;
	}

	public void setPropertyMappers ( Map<String, PropertyRdfMapper> propertyMappers ) {
		this.propertyMappers = propertyMappers;
	}
	
	/**
	 * Maps rdf:type for the class managed by this mapper ( i.e., T )
	 */
	public String getTargetRdfClassUri () {
		return targetRdfClassUri;
	}

	public void setRdfClassUri ( String targetRdfClassUri ) {
		this.targetRdfClassUri = targetRdfClassUri;
	}
	
	
	public RdfUriGenerator<T> getRdfIriGenerator () {
		return rdfUriGenerator;
	}

	public void setRdfUriGenerator ( RdfUriGenerator<T> rdfUriGenerator ) {
		this.rdfUriGenerator = rdfUriGenerator;
	}

	@Override
	public void setMapperFactory ( BeanRdfMapperFactory mapperFactory )
	{
		super.setMapperFactory ( mapperFactory );
		for ( PropertyRdfMapper pmapper: getPropertyMappers ().values () )
			pmapper.setMapperFactory ( mapperFactory );
	}
}
