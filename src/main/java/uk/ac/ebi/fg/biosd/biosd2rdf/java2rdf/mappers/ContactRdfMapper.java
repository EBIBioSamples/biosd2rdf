package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mappers.ToDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

/**
 * TODO: Comment me!
 * 
 * TODO: submission dc-terms:contributor contact
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
			/* TODO:
			 * ebi-terms:ContactPerson := 
  		 *   subClassOf ( 'is bearer of' (BFO_0000053) some 'contact representative role' (OBI_0001687) )
  		 *   subClassOf schema.org:Person, dcterms:Agent, foaf:Person
			 * 
			 */
			ns ( "ebi-terms", "ContactPerson" ), // TODO: See Schema design doc
			new MSIEquippedRdfUriGenerator<Contact> () 
			{
				@Override public String getUri ( Contact cnt ) 
				{
					String nameLine = trimToEmpty ( cnt.getFirstName () ) 
						+ trimToEmpty ( cnt.getMidInitials () ) 
						+ trimToEmpty ( cnt.getLastName () );
					nameLine = trimToNull ( nameLine );
					
					String email = trimToNull ( cnt.getEmail () );
					String id = email != null ? hashUriSignature ( email ) : getMsiAcc () + "/" + hashUriSignature ( nameLine );    
					
					return ns ( "biosd", "ref-contact/" + id );
			}}
		);
		
		Contact c = null;
		// TODO
		// schema.org properties, http://schema.rdfs.org/, 
		// also foaf properties
		// name + mid + surname will be dc:title/rdfs:comment
		
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<Contact, String> ( "email", ns ( "sch", "email" ) ) );
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<Contact, String> ( "firstName", ns ( "sch", "givenName" ) ) );
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<Contact, String> ( "midInitials", ns ( "sch", "additionalName" ) ) );
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<Contact, String> ( "lastName", ns ( "sch", "familyName" ) ) );
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<Contact, String> ( "phone", ns ( "sch", "telephone" ) ) );
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<Contact, String> ( "fax", ns ( "sch", "faxNumber" ) ) );
		
		// ebi-terms:has-address-line (subprop of dc:description/rdfs:comment)
		// TODO: possibly more annotations to be considered: 
		//   http://answers.semanticweb.com/questions/298/how-can-you-represent-physical-addresses-in-foaf
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<Contact, String> ( "address", ns ( "ebi-terms", "has-address-line" ) ) );
		
		// ebi-terms:has-affiliation-line (subprop of dc:description/rdfs:comment)
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<Contact, String> ( "affiliation", ns ( "ebi-terms", "has-affiliation-line" ) ) );
		
		// TODO: requires a special mapper that goes from string to URIs, without involving bean classes or URI generators
		// this.setPropertyMapper ( new ToObjectPropRdfMapper<Contact, String> ( "url", ns ( "rdfs", "seeAlso" ) ) );
		
		// TODO: These need to be checked against Zooma
		// c.getContactRoles ();
	}

	@Override
	public boolean map ( Contact cnt )
	{
		try
		{
			if ( super.map ( cnt ) ) return false;
			
			String nameLine = trimToEmpty ( cnt.getFirstName () ) 
				+ trimToEmpty ( cnt.getMidInitials () ) 
				+ trimToEmpty ( cnt.getLastName () );
			nameLine = trimToNull ( nameLine );

			OwlApiUtils.assertData ( this.getMapperFactory ().getKnowledgeBase (), 
				this.getRdfUriGenerator ().getUri ( cnt ), ns ( "dc-terms", "title" ), nameLine 
			);
			
			return true;
		} 
		catch ( Exception ex )
		{
			throw new RdfMappingException ( String.format ( "Error while mapping contact[%s]: %s", cnt, ex.getMessage () ), ex );
		}
	}
	
	
}
