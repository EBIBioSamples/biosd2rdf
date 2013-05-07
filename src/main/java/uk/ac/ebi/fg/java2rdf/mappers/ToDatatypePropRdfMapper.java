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
public class ToDatatypePropRdfMapper<T, PT> extends PropertyRdfMapper<T, PT>
{
	private RdfLiteralGenerator<PT> rdfLiteralGenerator;
	
	public ToDatatypePropRdfMapper ()  {
		this ( null, null, new RdfLiteralGenerator<PT> () );
	}

	public ToDatatypePropRdfMapper ( String sourcePropertyName, String targetPropertyUri )
	{
		this ( sourcePropertyName, targetPropertyUri, new RdfLiteralGenerator<PT> () );
	}

	public ToDatatypePropRdfMapper ( String sourcePropertyName, String targetPropertyUri, RdfLiteralGenerator<PT> rdfLiteralGenerator ) 
	{
		super( sourcePropertyName, targetPropertyUri );
		this.setRdfLiteralGenerator ( rdfLiteralGenerator );
	}
	
	/**
	 * Uses {@link #getRdfLiteralGenerator()} to generate an RDF value for the property target value. Then it generates 
	 * a triple where the property {@link #getSourcePropertyName()} is asserted for the source. 
	 * Uses {@link BeanRdfMapperFactory#getMapper(Object)} to get a mapper for the source and a URI from its 
	 * {@link BeanRdfMapper#getRdfIriGenerator()}.
	 */
	@Override
	public boolean map ( T source, PT propValue )
	{
		if ( source == null || propValue == null ) return false;
		
		try
		{
			BeanRdfMapperFactory mapFactory = this.getMapperFactory ();
			RdfLiteralGenerator<PT> targetValGen = this.getRdfLiteralGenerator ();
			
			OwlApiUtils.assertData ( this.getMapperFactory ().getKnowledgeBase (), 
				mapFactory.getUri ( source ), this.getTargetPropertyUri (), targetValGen.getValue ( propValue ) );
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
		return true;
	}

	public RdfLiteralGenerator<PT> getRdfLiteralGenerator () {
		return rdfLiteralGenerator;
	}

	public void setRdfLiteralGenerator ( RdfLiteralGenerator<PT> rdfLiteralGenerator ) {
		this.rdfLiteralGenerator = rdfLiteralGenerator;
	}
}
