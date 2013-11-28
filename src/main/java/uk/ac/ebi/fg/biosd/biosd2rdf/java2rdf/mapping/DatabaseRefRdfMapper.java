package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertLink;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CompositePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>7 Nov 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class DatabaseRefRdfMapper extends BeanRdfMapper<DatabaseRefSource>
{
	public DatabaseRefRdfMapper ()
	{
		super ( 
			ns ( "biosd-terms", "RepositoryWebRecord" ), 
	    new RdfUriGenerator<DatabaseRefSource>() {

				@Override
				public String getUri ( DatabaseRefSource db, Map<String, Object> params )
				{
					String acc = StringUtils.trimToNull ( db.getAcc () );
					if ( acc == null ) return null;
					
					String name = StringUtils.trimToNull ( db.getName () );
					if ( name == null ) return null;
										
					return ns ( "biosd", "repository-web-record/" + Java2RdfUtils.urlEncode ( name ) + ":" + acc );
				}
			}
		);

		this.addPropertyMapper ( "acc", new OwlDatatypePropRdfMapper<DatabaseRefSource, String> ( ns ( "dc-terms", "identifier" ) ) );
		this.addPropertyMapper ( "url", new OwlDatatypePropRdfMapper<DatabaseRefSource, String> ( ns ( "foaf", "page" ) ) );
		
		this.addPropertyMapper ( "description", new CompositePropRdfMapper<> (
			new OwlDatatypePropRdfMapper<DatabaseRefSource, String> ( ns ( "dc-terms", "description" ) ),
			new OwlDatatypePropRdfMapper<DatabaseRefSource, String> ( ns ( "rdfs", "comment" ) ) 
		));

		// Contains strings like 'PRIDE' or 'ArrayExpress', so dc:source this should be the best property to represent them
		this.addPropertyMapper ( "name", new CompositePropRdfMapper<> (  
			new OwlDatatypePropRdfMapper<DatabaseRefSource, String> ( ns ( "dc-terms", "source" ) ), 
			new OwlDatatypePropRdfMapper<DatabaseRefSource, String> ( ns ( "dc-terms", "title" ) ), 
			new OwlDatatypePropRdfMapper<DatabaseRefSource, String> ( ns ( "rdfs", "label" ) ) 
		));
	}

	@Override
	public boolean map ( DatabaseRefSource db, Map<String, Object> params )
	{
		if ( !super.map ( db, params ) ) return false;
			
		if ( !"ArrayExpress".equalsIgnoreCase ( db.getName () ) ) return true;
		
		// Build a URI that points at the contents of the Gene Expression Atlas data set. 
		// TODO: The Atlas actually contains a subset of ArrayExpress, we should use the endpoint (or the APIs) to know
		// if the URI is actually defined. For the moment we are like: if the resource existed, it would have this URI 
		// and that would return some RDF, when it doesn't exist you don't get any RDF back
		//
		
		String acc = StringUtils.trimToNull ( db.getAcc () );
		if ( acc == null ) return true;
		
		String atlasUri = "http://rdf.ebi.ac.uk/resource/atlas/" + acc;
		
		RdfMapperFactory mapf = this.getMapperFactory ();
		
		assertLink ( mapf.getKnowledgeBase (), atlasUri, ns ( "pav", "derivedFrom" ), mapf.getUri ( db, params ) );
		return true;
	}
	
}
