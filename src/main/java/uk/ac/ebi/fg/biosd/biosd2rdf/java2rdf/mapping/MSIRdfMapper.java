package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.uri;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CompositePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.InversePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlObjPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;


/**
 * Maps the submission of a SampleTab file to the BioSD database. 
 *
 * <dl><dt>date</dt><dd>Apr 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class MSIRdfMapper extends BeanRdfMapper<MSI>
{
	public MSIRdfMapper ()
	{
		super ( 
			uri ( "biosd-terms", "BiosamplesSubmission" ), // is-a iao:document 
			new RdfUriGenerator<MSI>() 
			{
				@Override
				public String getUri ( MSI msi, Map<String, Object> params ) { msi.getPublications ();
					return uri ( "biosd", "msi/" + urlEncode ( msi.getAcc () ) );
				}
		});

		this.addPropertyMapper ( "acc", new OwlDatatypePropRdfMapper<MSI, String> ( uri ( "dc-terms", "identifier" ) ) );
		
		this.addPropertyMapper ( "title", new CompositePropRdfMapper<> (  
			new OwlDatatypePropRdfMapper<MSI, String> ( uri ( "dc-terms", "title" ) ), 
			new OwlDatatypePropRdfMapper<MSI, String> ( uri ( "rdfs", "label" ) ) 
		));
		this.addPropertyMapper ( "description", new CompositePropRdfMapper<> (
			new OwlDatatypePropRdfMapper<MSI, String> ( uri ( "dc-terms", "description" ) ),
			new OwlDatatypePropRdfMapper<MSI, String> ( uri ( "rdfs", "comment" ) ) 
		));
		
		
		this.addPropertyMapper ( "samples", new CollectionPropRdfMapper<MSI, BioSample> ( 
			new OwlObjPropRdfMapper<MSI, BioSample> ( uri ( "obo", "IAO_0000219" ) ) ) // denotes
		);
		
		this.addPropertyMapper ( "sampleGroups", new CollectionPropRdfMapper<MSI, BioSampleGroup> ( 
			new OwlObjPropRdfMapper<MSI, BioSampleGroup> ( uri ( "obo", "IAO_0000219" ) ) ) // denotes
		);
		
		// 'is about', because we use an InversePropRdfMapper, this will generate (pub, is-about, msi) for every pub in 
		// msi.getPublications()
		this.addPropertyMapper ( "publications", new CollectionPropRdfMapper<MSI, Publication> ( 
			new InversePropRdfMapper<MSI, Publication> ( new CompositePropRdfMapper<Publication, MSI> ( 
				new OwlObjPropRdfMapper<Publication, MSI> ( uri ( "obo", "IAO_0000136" ) ), // is about
				new OwlObjPropRdfMapper<Publication, MSI> ( uri ( "sio", "SIO_000332" ) ) // is about 
		))));
		
		// a sub-property of ( (dc-terms:creator union dc-terms:contributor ) and ( schema.org:author union schema.org:contributor ) ) 
		this.addPropertyMapper ( "contacts", new CollectionPropRdfMapper<MSI, Contact> ( 
			new OwlObjPropRdfMapper<MSI, Contact> ( uri ( "biosd-terms", "has-knowledgeable-person" ) ) )
		);

		this.addPropertyMapper ( "organizations", new CollectionPropRdfMapper<MSI, Organization> ( 
			new OwlObjPropRdfMapper<MSI, Organization> ( uri ( "biosd-terms", "has-knowledgeable-organization" ) ) )
		);

		this.addPropertyMapper ( "databaseRecordRefs", new CollectionPropRdfMapper<> ( new CompositePropRdfMapper<> ( 
			new OwlObjPropRdfMapper<MSI, DatabaseRecordRef> ( uri ( "pav", "derivedFrom" ) ),
			// dbrecord denotes submission
			new InversePropRdfMapper<MSI, DatabaseRecordRef> ( 
				new OwlObjPropRdfMapper<DatabaseRecordRef, MSI> ( uri ( "obo", "IAO_0000219" ) ) 
			)
		)));

		// TODO: more
	}

	@Override
	public boolean map ( MSI msi, Map<String, Object> params )
	{
		try
		{
			String msiAcc = StringUtils.trimToNull ( msi.getAcc () );
			if ( msiAcc == null ) return false; 

			if ( params == null ) params = new HashMap<String, Object> ();
			params.put ( "msiAccession", msi.getAcc () );

			boolean result = super.map ( msi, params );
			
			log.trace ( "Submission {} mapping done", msi.getAcc () );
			return result;
		} 
		catch ( Exception ex ) {
			throw new RdfMappingException ( String.format ( 
				"Error while mapping SampleTab submission[%s]: %s", msi.getAcc(), ex.getMessage () ), ex );
		}
	}
	
}
