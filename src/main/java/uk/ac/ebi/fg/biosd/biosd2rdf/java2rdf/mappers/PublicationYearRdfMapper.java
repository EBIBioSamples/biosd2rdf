package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import org.apache.commons.lang.StringUtils;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mappers.RdfLiteralGenerator;
import uk.ac.ebi.fg.java2rdf.mappers.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * TODO: Comment me!
 *
 * TODO: Literal Generator and Data Prop mapper should be generalised, so that they can deal with
 * data type objects and literal objects from OWLAPI. Then a specialisation for the string case should be derived.  
 *
 * <dl><dt>date</dt><dd>25 Jun 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class PublicationYearRdfMapper extends OwlDatatypePropRdfMapper<Publication, String>
{
	public PublicationYearRdfMapper () {
		super ( ns ( "fabio", "hasPublicationYear" ) );
	}
	
	@Override
	public boolean map ( Publication source, String year )
	{
		try
		{
			if ( source == null || year == null ) return false;
			RdfMapperFactory mapFactory = this.getMapperFactory ();
			RdfLiteralGenerator<String> targetValGen = this.getRdfLiteralGenerator ();
			
			OwlApiUtils.assertData ( this.getMapperFactory ().getKnowledgeBase (), 
				mapFactory.getUri ( source ), 
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
