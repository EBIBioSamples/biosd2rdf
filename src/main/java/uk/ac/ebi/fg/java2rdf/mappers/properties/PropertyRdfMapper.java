/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.mappers.properties;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMapperFactory;


/**
 * TODO: COMMENT ME AGAIN!!!
 * 
 * Property mappers are used to associate a JavaBean property to an RDFS/OWL property. Property mappers are usually
 * invoked by {@link BeanRdfMapper#map(Object)}.
 * 
 * @param <T> the type of source mapped bean which of property is mapped to RDF.
 * @param <PT> the type of source property that is mapped to RDF.
 *
 * TODO: do we need a field like this.specificRDFValueMapper? 
 * 
 * <dl><dt>date</dt><dd>Mar 24, 2013</dd></dl>
 * @author Marco Brandizi
 */
public abstract class PropertyRdfMapper<T, PT> extends RdfMapper<T>
{
	private String targetPropertyUri;
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public PropertyRdfMapper ()
	{
		super ();
	}


	public PropertyRdfMapper ( String targetPropertyUri )
	{
		super ();
		this.setTargetPropertyUri ( targetPropertyUri );
	}
	

	
	/**
	 * <p>Implements a specific way to map the property value of this source.
	 * You should take care of the case propValue == null, while {@link #map(Object)} takes care of the null source.</p>
	 * 
	 * <p>This usually creates a statement having the source as subject (typically uses {@link RdfMapperFactory#getRdfUriGenerator(Object)}
	 * for its URI), {@link #getTargetPropertyUri()} as RDF/OWL property and a value or another URI as object (again, it
	 * usually uses the factory for that).</p>
	 * 
	 * <p>As usually, it returns true when a real addition occurs.</p>
	 */
	public boolean map ( T source, PT propValue ) {
		if ( propValue == null ) return false;
		return true;
	}

	public String getTargetPropertyUri () {
		return targetPropertyUri;
	}

	public void setTargetPropertyUri ( String targetPropertyUri ) {
		this.targetPropertyUri = targetPropertyUri;
	}
}
