package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertLink;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CompositePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.UriStringPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>7 Nov 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class DbRecRefRdfMapper extends BeanRdfMapper<DatabaseRecordRef>
{
	public DbRecRefRdfMapper ()
	{
		super ( 
			ns ( "biosd-terms", "RepositoryWebRecord" ), 
	    new RdfUriGenerator<DatabaseRecordRef>() {

				@Override
				public String getUri ( DatabaseRecordRef db, Map<String, Object> params )
				{
					String acc = StringUtils.trimToNull ( db.getAcc () );
					if ( acc == null ) return null;
					
					String name = StringUtils.trimToNull ( db.getDbName () );
					if ( name == null ) return null;
										
					return ns ( "biosd", "repository-web-record/" + Java2RdfUtils.urlEncode ( name ) + ":" + acc );
				}
			}
		);

		this.addPropertyMapper ( "acc", new OwlDatatypePropRdfMapper<DatabaseRecordRef, String> ( ns ( "dc-terms", "identifier" ) ) );
		this.addPropertyMapper ( "url", new UriStringPropRdfMapper<DatabaseRecordRef> ( ns ( "foaf", "page" ), true ) );
		
		this.addPropertyMapper ( "description", new CompositePropRdfMapper<> (
			new OwlDatatypePropRdfMapper<DatabaseRecordRef, String> ( ns ( "dc-terms", "description" ) ),
			new OwlDatatypePropRdfMapper<DatabaseRecordRef, String> ( ns ( "rdfs", "comment" ) ) 
		));

		// Contains strings like 'PRIDE' or 'ArrayExpress', so dc:source this should be the best property to represent them
		this.addPropertyMapper ( "name",  new OwlDatatypePropRdfMapper<DatabaseRecordRef, String> ( ns ( "dc-terms", "source" ) ) ); 
	}

	@Override
	public boolean map ( DatabaseRecordRef db, Map<String, Object> params )
	{
		if ( !super.map ( db, params ) ) return false;
		
		RdfMapperFactory mapf = this.getMapperFactory ();
		OWLOntology kb = mapf.getKnowledgeBase ();

		// A composed string for the title
		String title = db.getDbName () + ":" + db.getAcc ();
		RdfUriGenerator<DatabaseRecordRef> uriGen = this.getRdfUriGenerator ();
		assertData ( kb, uriGen.getUri ( db, params ), ns ( "dc-terms", "title" ), title );
		assertData ( kb, uriGen.getUri ( db, params ), ns ( "rdfs", "label" ), title );
		
		
		if ( !"ArrayExpress".equalsIgnoreCase ( db.getDbName () ) ) return true;
		
		// Build a URI that points at the contents of the Gene Expression Atlas data set. 
		// TODO: The Atlas actually contains a subset of ArrayExpress, we should use the endpoint (or the APIs) to know
		// if the URI is actually defined. For the moment we are like: if the resource existed, it would have this URI 
		// and that would return some RDF, when it doesn't exist you don't get any RDF back
		//
				
		String atlasUri = "http://rdf.ebi.ac.uk/resource/atlas/" + db.getAcc ();
		assertLink ( kb, atlasUri, ns ( "pav", "derivedFrom" ), mapf.getUri ( db, params ) );

		return true;
	}
	
}
