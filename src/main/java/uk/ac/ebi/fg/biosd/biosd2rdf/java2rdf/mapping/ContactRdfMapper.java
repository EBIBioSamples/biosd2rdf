package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.Map;

import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.UriStringPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * Maps a contact in a BioSD/SampleTab submission to RDF. This will be a specific OWL class, which accounts for 
 * the fact that there is a person who is somehow involved in the submission, for instance as PI, submitter or 
 * data curator. Neither BioSD or SampleTab allows us to be more specific about such contacts.
 *
 * <dl><dt>date</dt><dd>Jun 18, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class ContactRdfMapper extends BeanRdfMapper<Contact>
{
	public ContactRdfMapper ()
	{
		super (
			ns ( "biosd-terms", "ContactPerson" ),
			new RdfUriGenerator<Contact> () 
			{
				@Override public String getUri ( Contact cnt, Map<String, Object> params ) 
				{
					String nameLine = trimToEmpty ( cnt.getFirstName () ) 
						+ trimToEmpty ( cnt.getMidInitials () ) 
						+ trimToEmpty ( cnt.getLastName () );
					nameLine = trimToNull ( nameLine );
					
					String email = trimToNull ( cnt.getEmail () );
					String id = email != null 
						? hashUriSignature ( email ) 
						: nameLine == null 
						  ? null : urlEncode ( (String) params.get ( "msiAccession" ) ) + "/" + hashUriSignature ( nameLine );    
					
					return id == null ? null : ns ( "biosd", "ref-contact/" + id );
			}}
		);
		
		// TODO
		// schema.org properties, http://schema.rdfs.org/, 
		// also foaf properties
		// name + mid + surname will be dc:title/rdfs:comment
		
		this.addPropertyMapper ( "email", new OwlDatatypePropRdfMapper<Contact, String> ( ns ( "sch", "email" ) ) );
		this.addPropertyMapper ( "firstName", new OwlDatatypePropRdfMapper<Contact, String> ( ns ( "sch", "givenName" ) ) );
		this.addPropertyMapper ( "midInitials", new OwlDatatypePropRdfMapper<Contact, String> ( ns ( "sch", "additionalName" ) ) );
		this.addPropertyMapper ( "lastName", new OwlDatatypePropRdfMapper<Contact, String> ( ns ( "sch", "familyName" ) ) );
		this.addPropertyMapper ( "phone", new OwlDatatypePropRdfMapper<Contact, String> ( ns ( "sch", "telephone" ) ) );
		this.addPropertyMapper ( "phone", new OwlDatatypePropRdfMapper<Contact, String> ( ns ( "sch", "faxNumber" ) ) );
		
		// biosd-terms:has-address-line (subprop of dc:description/rdfs:comment)
		// TODO: possibly more annotations to be considered: 
		//   http://answers.semanticweb.com/questions/298/how-can-you-represent-physical-addresses-in-foaf
		this.addPropertyMapper ( "address", new OwlDatatypePropRdfMapper<Contact, String> ( ns ( "biosd-terms", "has-address-line" ) ) );
		
		// biosd-terms:has-affiliation-line (subprop of dc:description/rdfs:comment)
		this.addPropertyMapper ( "affiliation", new OwlDatatypePropRdfMapper<Contact, String> ( ns ( "biosd-terms", "has-affiliation-line" ) ) );
		this.addPropertyMapper ( "url", new UriStringPropRdfMapper<Contact> ( ns ( "foaf", "page" ), true ) );
		
		// TODO: These need to be checked against Zooma
		// c.getContactRoles ();
	}

	@Override
	public boolean map ( Contact cnt, Map<String, Object> params )
	{
		try
		{
			if ( super.map ( cnt, params ) ) return false;
			
			String nameLine = trimToEmpty ( cnt.getFirstName () ) 
				+ trimToEmpty ( cnt.getMidInitials () ) 
				+ trimToEmpty ( cnt.getLastName () );
			nameLine = trimToNull ( nameLine );

			OwlApiUtils.assertData ( this.getMapperFactory ().getKnowledgeBase (), 
				this.getRdfUriGenerator ().getUri ( cnt ), ns ( "dc-terms", "title" ), nameLine 
			);
			OwlApiUtils.assertData ( this.getMapperFactory ().getKnowledgeBase (), 
				this.getRdfUriGenerator ().getUri ( cnt ), ns ( "rdfs", "label" ), nameLine 
			);
			
			return true;
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( "Error while mapping contact[%s]: %s", cnt, ex.getMessage () ), ex );
		}
	}
	
	
}
