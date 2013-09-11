package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.Map;

import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.RdfMappingException;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;
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
  		 *   ebi-terms:Person := subClassOf ( schema.org:Person, dcterms:Agent, foaf:Person )
			 * 
			 */
			ns ( "biosd-terms", "ContactPerson" ), // TODO: See Schema design doc
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
						: urlEncode ( (String) params.get ( "msiAccession" ) ) + "/" + hashUriSignature ( nameLine );    
					
					return ns ( "biosd", "ref-contact/" + id );
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
		
		// TODO: requires a special mapper that goes from string to URIs, without involving bean classes or URI generators
		// this.setPropertyMapper ( new OwlObjPropRdfMapper<Contact, String> ( "url", ns ( "rdfs", "seeAlso" ) ) );
		
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
