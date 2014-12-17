package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertData;
import static uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils.assertLink;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CompositePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.UriStringPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingManager;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.EntityMappingSearchResult;
import uk.ac.ebi.fg.myequivalents.managers.interfaces.ManagerFactory;
import uk.ac.ebi.fg.myequivalents.model.Entity;
import uk.ac.ebi.fg.myequivalents.resources.Resources;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>7 Nov 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class DbRecRefRdfMapper extends BeanRdfMapper<DatabaseRecordRef>
{
	private static Logger log = LoggerFactory.getLogger ( DbRecRefRdfMapper.class );
	
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
					// TODO: version
					return ns ( "biosd", 
						"repository-web-record/" + Java2RdfUtils.urlEncode ( name ) + ":" + Java2RdfUtils.urlEncode ( acc ) );
				}
			}
		);

		this.addPropertyMapper ( "acc", new OwlDatatypePropRdfMapper<DatabaseRecordRef, String> ( ns ( "dc-terms", "identifier" ) ) );
		this.addPropertyMapper ( "url", new UriStringPropRdfMapper<DatabaseRecordRef> ( ns ( "foaf", "page" ), true ) );
		
		this.addPropertyMapper ( "title", new CompositePropRdfMapper<> (
			new OwlDatatypePropRdfMapper<DatabaseRecordRef, String> ( ns ( "dc-terms", "description" ) ),
			new OwlDatatypePropRdfMapper<DatabaseRecordRef, String> ( ns ( "rdfs", "comment" ) ) 
		));

		// Contains strings like 'PRIDE' or 'ArrayExpress', so dc:source this should be the best property to represent them
		this.addPropertyMapper ( "dbName",  new OwlDatatypePropRdfMapper<DatabaseRecordRef, String> ( ns ( "dc-terms", "source" ) ) ); 
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
	
	/**
	 * TODO: comment me!
	 */
	public static DatabaseRecordRef[] getMyEquivalentsLinks ( String serviceName, String acc )
	{
		EntityMappingManager myeqMapMgr = null;
		
		try
		{
			ManagerFactory myeqMgrFact = Resources.getInstance ().getMyEqManagerFactory ();
			myeqMapMgr = myeqMgrFact.newEntityMappingManager ();
			
			Collection<EntityMappingSearchResult.Bundle> bundles = myeqMapMgr.getMappings ( false, serviceName + ":" + acc ).getBundles ();
			if ( bundles.isEmpty () ) return new DatabaseRecordRef [ 0 ];

			EntityMappingSearchResult.Bundle bundle = bundles.iterator ().next ();
			Set<Entity> entities = bundle.getEntities ();
			DatabaseRecordRef result[] = new DatabaseRecordRef [ bundle.getEntities ().size () - 1 ];
			int iresult = 0;
			
			for ( Entity entity: entities )
			{
				String eServiceName = entity.getServiceName (), eAcc = entity.getAccession ();
				if ( serviceName.equals ( eServiceName ) && acc.equals ( eAcc ) ) continue;
				
				String eServiceNameCleaned = eServiceName;
				if ( eServiceName.endsWith ( ".samples" ) )
					eServiceNameCleaned = StringUtils.substring ( eServiceName, 0, - ".samples".length () );
				else if ( eServiceName.endsWith ( ".groups" ) )
					eServiceNameCleaned = StringUtils.substring ( eServiceName, 0, - ".groups".length () );
				
				DatabaseRecordRef dbxref = new DatabaseRecordRef ( 
					eServiceNameCleaned, eAcc, null, entity.getURI (), entity.getService ().getTitle () 
				);
				
				result [ iresult++ ] = dbxref;
				
			}
			
			return result;
		}
		catch ( RuntimeException ex )
		{
			log.error ( String.format ( 
				"Error while contacting myEquivalents for '%s:%s': %s, returning null", serviceName, acc, ex.getMessage () ), 
				ex	
			);
			return new DatabaseRecordRef [ 0 ];
		}
		finally {
			if ( myeqMapMgr != null ) myeqMapMgr.close ();
		}
	}

	
}
