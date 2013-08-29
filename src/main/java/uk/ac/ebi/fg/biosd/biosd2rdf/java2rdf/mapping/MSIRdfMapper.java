package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMapperFactory;
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
			ns ( "biosd", "Submission" ), // TODO: is-a iao:document 
			new RdfUriGenerator<MSI>() 
			{
				@Override
				public String getUri ( MSI msi ) { msi.getPublications ();
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
		
		// TODO: more
		
		this.addPropertyMapper ( "samples", new CollectionPropRdfMapper<MSI, BioSample> ( 
			new OwlObjPropRdfMapper<MSI, BioSample> ( ns ( "obo", "IAO_0000219" ) ) ) // denotes
		);
		
		this.addPropertyMapper ( "sampleGroups", new CollectionPropRdfMapper<MSI, BioSampleGroup> ( 
			new OwlObjPropRdfMapper<MSI, BioSampleGroup> ( ns ( "obo", "IAO_0000219" ) ) ) // denotes
		);
		
		// 'is about' (used in (pub, is-about, msi))
		this.addPropertyMapper ( "publications", new CollectionPropRdfMapper<MSI, Publication> ( 
			new InversePropRdfMapper<MSI, Publication> ( 
				new OwlObjPropRdfMapper<Publication, MSI> ( ns ( "obo", "IAO_0000136" ) ) ) 
		));
		
		// TODO: sub-property of ( (dc-terms:creator union dc-terms:contributor ) and ( schema.org:author union schema.org:contributor ) ) 
		this.addPropertyMapper ( "contacts", new CollectionPropRdfMapper<MSI, Contact> ( 
			new OwlObjPropRdfMapper<MSI, Contact> ( ns ( "ebi-terms", "has-knowledgeable-person" ) ) )
		);

		this.addPropertyMapper ( "organizations", new CollectionPropRdfMapper<MSI, Organization> ( 
			new OwlObjPropRdfMapper<MSI, Organization> ( ns ( "ebi-terms", "has-knowledgeable-organization" ) ) )
		);

	}

	@Override
	public boolean map ( MSI msi )
	{
		try
		{
			String msiAcc = StringUtils.trimToNull ( msi.getAcc () );
			if ( msiAcc == null ) return false; 
			
			RdfMapperFactory mapFact = this.getMapperFactory ();
			
			((MSIEquippedRdfUriGenerator<Contact>) mapFact.getRdfUriGenerator ( Contact.class )).setMsiAcc ( msiAcc );
			((MSIEquippedRdfUriGenerator<Organization>) mapFact.getRdfUriGenerator ( Organization.class )).setMsiAcc ( msiAcc );

			return super.map ( msi );
		} 
		catch ( Exception ex ) {
			throw new RdfMappingException ( String.format ( 
				"Error while mapping SampleTab submission[%s]: %s", msi.getAcc(), ex.getMessage () ), ex );
		}
	}
	
}
