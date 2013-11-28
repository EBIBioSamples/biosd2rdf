package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;
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
	@SuppressWarnings ( "unchecked" )
	public MSIRdfMapper ()
	{
		super ( 
			ns ( "biosd-terms", "BiosamplesSubmission" ), // is-a iao:document 
			new RdfUriGenerator<MSI>() 
			{
				@Override
				public String getUri ( MSI msi, Map<String, Object> params ) { msi.getPublications ();
					return ns ( "biosd", "msi/" + urlEncode ( msi.getAcc () ) );
				}
		});

		this.addPropertyMapper ( "acc", new OwlDatatypePropRdfMapper<MSI, String> ( ns ( "dc-terms", "identifier" ) ) );
		
		this.addPropertyMapper ( "title", new CompositePropRdfMapper<> (  
			new OwlDatatypePropRdfMapper<MSI, String> ( ns ( "dc-terms", "title" ) ), 
			new OwlDatatypePropRdfMapper<MSI, String> ( ns ( "rdfs", "label" ) ) 
		));
		this.addPropertyMapper ( "description", new CompositePropRdfMapper<> (
			new OwlDatatypePropRdfMapper<MSI, String> ( ns ( "dc-terms", "description" ) ),
			new OwlDatatypePropRdfMapper<MSI, String> ( ns ( "rdfs", "comment" ) ) 
		));
		
		
		this.addPropertyMapper ( "samples", new CollectionPropRdfMapper<MSI, BioSample> ( 
			new OwlObjPropRdfMapper<MSI, BioSample> ( ns ( "obo", "IAO_0000219" ) ) ) // denotes
		);
		
		this.addPropertyMapper ( "sampleGroups", new CollectionPropRdfMapper<MSI, BioSampleGroup> ( 
			new OwlObjPropRdfMapper<MSI, BioSampleGroup> ( ns ( "obo", "IAO_0000219" ) ) ) // denotes
		);
		
		// 'is about', because we use an InversePropRdfMapper, this will generate (pub, is-about, msi) for every pub in 
		// msi.getPublications()
		this.addPropertyMapper ( "publications", new CollectionPropRdfMapper<MSI, Publication> ( 
			new InversePropRdfMapper<MSI, Publication> ( 
				new OwlObjPropRdfMapper<Publication, MSI> ( ns ( "obo", "IAO_0000136" ) ) ) 
		));
		
		// a sub-property of ( (dc-terms:creator union dc-terms:contributor ) and ( schema.org:author union schema.org:contributor ) ) 
		this.addPropertyMapper ( "contacts", new CollectionPropRdfMapper<MSI, Contact> ( 
			new OwlObjPropRdfMapper<MSI, Contact> ( ns ( "biosd-terms", "has-knowledgeable-person" ) ) )
		);

		this.addPropertyMapper ( "organizations", new CollectionPropRdfMapper<MSI, Organization> ( 
			new OwlObjPropRdfMapper<MSI, Organization> ( ns ( "biosd-terms", "has-knowledgeable-organization" ) ) )
		);

		this.addPropertyMapper ( "databases", new CollectionPropRdfMapper<> ( new CompositePropRdfMapper<> ( 
			new OwlObjPropRdfMapper<MSI, DatabaseRefSource> ( ns ( "pav", "derivedFrom" ) ),
			// dbrecord denotes submission
			new InversePropRdfMapper<MSI, DatabaseRefSource> ( 
				new OwlObjPropRdfMapper<DatabaseRefSource, MSI> ( ns ( "obo", "IAO_0000219" ) ) 
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

			return super.map ( msi, params );
		} 
		catch ( Exception ex ) {
			throw new RdfMappingException ( String.format ( 
				"Error while mapping SampleTab submission[%s]: %s", msi.getAcc(), ex.getMessage () ), ex );
		}
	}
	
}
