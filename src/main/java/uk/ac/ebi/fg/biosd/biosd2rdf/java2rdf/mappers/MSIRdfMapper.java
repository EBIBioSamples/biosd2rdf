package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mappers.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.mappers.ToDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.ToObjectInversePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.ToObjectPropRdfMapper;

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
			ns ( "biosd", "Submission" ), // TODO: is-a iao:document 
			new RdfUriGenerator<MSI>() 
			{
				@Override
				public String getUri ( MSI msi ) { msi.getPublications ();
					return ns ( "biosd", "msi/" + msi.getAcc () );
				}
		});
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<MSI, String> ( "title", ns ( "dc-terms", "title" ) ) );
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<MSI, String> ( "description", ns ( "dc-terms", "description" ) ) );
		
		// TODO: more
		
		this.setPropertyMapper ( new CollectionPropRdfMapper<MSI, BioSample> ( 
			"samples", ns ( "obo", "IAO_0000219" ), new ToObjectPropRdfMapper<MSI, BioSample> () ) // denotes
		);
		
		this.setPropertyMapper ( new CollectionPropRdfMapper<MSI, BioSampleGroup> ( 
			"sampleGroups", ns ( "obo", "IAO_0000219" ), new ToObjectPropRdfMapper<MSI, BioSampleGroup> () ) // denotes
		);
		
		// 'is about' (used in (pub, is-about, msi))
		this.setPropertyMapper ( new CollectionPropRdfMapper<MSI, Publication> ( 
			"publications", ns ( "obo", "IAO_0000136" ), new ToObjectInversePropRdfMapper<MSI, Publication> () )
		);
		
		// TODO: sub-property of ( (dc-terms:creator union dc-terms:contributor ) and ( schema.org:author union schema.org:contributor ) ) 
		this.setPropertyMapper ( new CollectionPropRdfMapper<MSI, Contact> ( 
			"contacts", ns ( "ebi-terms", "has-knowledgeable-person" ), new ToObjectPropRdfMapper<MSI, Contact> () )
		);

		this.setPropertyMapper ( new CollectionPropRdfMapper<MSI, Organization> ( 
			"organizations", ns ( "ebi-terms", "has-knowledgeable-organization" ), new ToObjectPropRdfMapper<MSI, Organization> () )
		);

	}

	@Override
	@SuppressWarnings ( { "rawtypes", "unchecked" } )
	public boolean map ( MSI msi )
	{
		try
		{
			String msiAcc = StringUtils.trimToNull ( msi.getAcc () );
			if ( msiAcc == null ) return false; 
			
			Map<Class, BeanRdfMapper> mappers = this.getMapperFactory ().getMappers ();
			((MSIEquippedRdfUriGenerator<Contact>) mappers.get ( Contact.class ).getRdfUriGenerator ()).setMsiAcc ( msiAcc );
			((MSIEquippedRdfUriGenerator<Organization>) mappers.get ( Organization.class ).getRdfUriGenerator ()).setMsiAcc ( msiAcc );

			return super.map ( msi );
		} 
		catch ( Exception ex ) {
			throw new RdfMappingException ( String.format ( "Error while mapping SampleTab submission[%s]: %s", msi, ex.getMessage () ), ex );
		}
	}
	
}
