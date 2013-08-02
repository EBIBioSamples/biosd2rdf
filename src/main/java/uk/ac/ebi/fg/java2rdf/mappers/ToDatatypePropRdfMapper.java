/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * This maps a JavaBean property to an OWL data-type property. It uses {@link #getRdfLiteralGenerator()} to get the 
 * literal value that has to be used as the value for this property. For instance, this could be used in a {@link BeanRdfMapper}
 * about the Book Java class, to generate statements like http://rdf.example.com/isbn/123 dc:title 'I, Robot'. A 
 * {@link RdfUriGenerator} will be used for the rdf.example.com part, a data type mapper for 
 * dc:title (given by {@link #getSourcePropertyName()} and a {@link RdfLiteralGenerator} for extracting the title 
 * value 'I, Robot'.
 * 
 * @See {@link RdfLiteralGenerator}.
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
	 * {@link BeanRdfMapper#getRdfUriGenerator()}.
	 */
	@Override
	public boolean map ( T source, PT propValue )
	{
		try
		{
			if ( propValue == null ) return false;
			
			BeanRdfMapperFactory mapFactory = this.getMapperFactory ();
			RdfLiteralGenerator<PT> targetValGen = this.getRdfLiteralGenerator ();
			String targetRdfVal = targetValGen.getValue ( propValue );
			if ( targetRdfVal == null ) return false;
			
			OwlApiUtils.assertData ( this.getMapperFactory ().getKnowledgeBase (), 
				mapFactory.getUri ( source ), this.getTargetPropertyUri (), targetRdfVal );
			return true;
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Internal error while mapping %s[%s].'%s'='%s' to RDF: %s", 
				source.getClass ().getSimpleName (), 
				StringUtils.abbreviate ( source.toString (), 50 ), this.getSourcePropertyName (), 
				StringUtils.abbreviate ( propValue.toString(), 50 ),
				ex.getMessage ()
			), ex);
		}
	}

	/**
	 * This generates the literal value (a plain string at the moment) to be used to map an object which is the value
	 * of a JavaBean property into a string representation of such value. 
	 */
	public RdfLiteralGenerator<PT> getRdfLiteralGenerator () {
		return rdfLiteralGenerator;
	}

	public void setRdfLiteralGenerator ( RdfLiteralGenerator<PT> rdfLiteralGenerator ) {
		this.rdfLiteralGenerator = rdfLiteralGenerator;
	}
}
