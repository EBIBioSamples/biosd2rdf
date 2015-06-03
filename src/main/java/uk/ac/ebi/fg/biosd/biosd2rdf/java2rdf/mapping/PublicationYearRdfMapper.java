package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.uri;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfLiteralGenerator;
import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * A special data type property mapper that is intented to translate {@link Publication#getYear()} into
 * a literal of type {@link XSDVocabulary#G_YEAR}. 
 * 
 * <dl><dt>date</dt><dd>25 Jun 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class PublicationYearRdfMapper extends OwlDatatypePropRdfMapper<Publication, String>
{
	public PublicationYearRdfMapper () {
		super ( uri ( "fabio", "hasPublicationYear" ) );
	}
	
	@Override
	public boolean map ( Publication source, String year, Map<String, Object> params )
	{
		try
		{
			Validate.notNull ( source, "Internal error: cannot map a null publication to RDF" );

			RdfMapperFactory mapFactory = this.getMapperFactory ();
			Validate.notNull ( mapFactory, "Internal error: %s must be linked to a mapper factory", this.getClass ().getSimpleName () );
			
			String subjUri = mapFactory.getUri ( source, params );
			if ( subjUri == null ) return false;

			if ( ( year = StringUtils.trimToNull ( year ) ) == null ) return false;

			RdfLiteralGenerator<String> targetValGen = this.getRdfLiteralGenerator ();
			
			OwlApiUtils.assertData ( this.getMapperFactory ().getKnowledgeBase (), 
				mapFactory.getUri ( source, params ), 
				this.getTargetPropertyUri (), 
				targetValGen.getValue ( year ), 
				XSDVocabulary.G_YEAR.toString () );
			return true;
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( 
				"Error while doing the RDF mapping <%s[%s] '%s' '%s': %s", 
				source.getClass ().getSimpleName (), 
				StringUtils.abbreviate ( source.toString (), 50 ), 
				this.getTargetPropertyUri (),
				StringUtils.abbreviate ( year, 50 ), 
				ex.getMessage ()
			), ex );
		}
	}
}
