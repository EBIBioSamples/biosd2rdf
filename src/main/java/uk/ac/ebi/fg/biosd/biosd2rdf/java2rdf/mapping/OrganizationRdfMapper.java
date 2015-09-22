package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.uri;

import java.util.Map;

import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CompositePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.UriStringPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;

/**
 * Maps a contact organisation in a BioSD/SampleTab submission to RDF. This will be a specific OWL class, which accounts for 
 * the fact that there is an organisation that is somehow involved in the submission, for instance as PI, submitters or 
 * data curators. Neither BioSD or SampleTab allows us to be more specific about such type of contact.
 *
 * <dl><dt>date</dt><dd>Jun 18, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class OrganizationRdfMapper extends BeanRdfMapper<Organization>
{
	public OrganizationRdfMapper ()
	{
		super (
			uri ( "biosd-terms", "ContactOrganization" ), 
			new RdfUriGenerator<Organization>()
			{
				@Override public String getUri ( Organization org, Map<String, Object> params ) 
				{
					String name = trimToNull ( org.getName () );
					if ( name == null ) return null;
					String id = trimToNull ( org.getEmail () );
					id = id != null 
						? hashUriSignature ( id ) 
						: urlEncode ( (String) params.get ( "msiAccession" ) ) + "/" + hashUriSignature ( name );    
					
					return uri ( "biosd", "organization/" + id );
			}}
		);
		
		this.addPropertyMapper ( "email", new OwlDatatypePropRdfMapper<Organization, String> ( uri ( "sch", "email" ) ) );
		this.addPropertyMapper ( "name", new CompositePropRdfMapper<> ( 
			new OwlDatatypePropRdfMapper<Organization, String> ( uri ( "sch", "name" ) ),
			new OwlDatatypePropRdfMapper<Organization, String> ( uri ( "dc-terms", "title" ) ),
			new OwlDatatypePropRdfMapper<Organization, String> ( uri ( "rdfs", "label" ) )
		));
		this.addPropertyMapper ( "description", new CompositePropRdfMapper<> (
			new OwlDatatypePropRdfMapper<Organization, String> ( uri ( "sch", "description" ) ),
			new OwlDatatypePropRdfMapper<Organization, String> ( uri ( "dc-terms", "description" ) ),
			new OwlDatatypePropRdfMapper<Organization, String> ( uri ( "rdfs", "comment" ) ) 
		));
		this.addPropertyMapper ( "address", new OwlDatatypePropRdfMapper<Organization, String> ( uri ( "biosd-terms", "has-address-line" ) ) );
		this.addPropertyMapper ( "url", new UriStringPropRdfMapper<Organization> ( uri ( "foaf", "page" ), true ) );
		
		// TODO: These need to be checked against the discoverer
		// o.getRole ();
	}
}
