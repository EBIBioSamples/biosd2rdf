package uk.ac.ebi.fg.java2rdf.mappers.properties;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.java2rdf.mappers.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * It works like {@link OwlObjPropRdfMapper}, but for inverse links, i.e., instead of triples 
 * of type (T, {@link #getTargetPropertyUri()}, PT), it builds triples of type (PT, {@link #getTargetPropertyUri()}, T)
 * for the value found in {@link #getSourcePropertyName()}.
 * 
 * TODO: data properties need a corresponding mapper.
 *
 * <dl><dt>date</dt><dd>25 Jun 2013</dd></dl>
 * @author Marco Brandizi
 */
public class InverseOwlObjPropRdfMapper<T, PT> extends OwlObjPropRdfMapper<T, PT>
{
	public InverseOwlObjPropRdfMapper ()  {
		super ();
	}

	public InverseOwlObjPropRdfMapper ( String targetPropertyUri ) {
		super ( targetPropertyUri );
	}
	
	
	/**
	 * Generates a triple where the property {@link #getSourcePropertyName()} is asserted for the source, using
	 * {@link #getTargetPropertyUri()}. Uses {@link RdfMapperFactory#getUri(Object)} for both the source and the target URI. 
	 */
	@Override
	public boolean map ( T source, PT propValue )
	{
		try
		{
			if ( propValue == null ) return false;
			RdfMapperFactory mapFactory = this.getMapperFactory ();
			
			String propValUri = mapFactory.getUri ( propValue );
			if ( propValUri == null ) return false;
			
			OwlApiUtils.assertLink ( this.getMapperFactory ().getKnowledgeBase (), 
				propValUri, this.getTargetPropertyUri (), mapFactory.getUri ( source ) );

			// Don't use targetMapper, we need to trace this visit.
			return mapFactory.map ( propValue );
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Error while doing the RDF mapping <%s[%s] '%s' [%s]: %s", 
					source.getClass ().getSimpleName (), 
					StringUtils.abbreviate ( propValue.toString (), 50 ), 
					this.getTargetPropertyUri (),
					StringUtils.abbreviate ( source.toString (), 50 ), 
					ex.getMessage ()
			), ex );
		}
	}
	
}
