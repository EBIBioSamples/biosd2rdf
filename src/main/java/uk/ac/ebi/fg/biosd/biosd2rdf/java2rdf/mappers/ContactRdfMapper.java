package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;

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
			 * ebi-terms:ContactReference := 
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
					String id = email != null ? hashUriSignature ( email ) : getMsiAcc () + "/" + nameLine;    
					
					return ns ( "biosd", "ref-contact/" + getMsiAcc () + "/" + id );
			}}
		);
		
		Contact c = null;
		// schema.org properties, http://schema.rdfs.org/, 
		// also foaf properties
		// name + mid + surname will be dc:title/rdfs:comment
		c.getEmail ();
		c.getFirstName ();
		c.getMidInitials ();
		c.getLastName ();
		c.getPhone ();
		c.getFax ();
		
		
		// ebi-terms:has-address-line (subprop of dc:description/rdfs:comment)
		// TODO: possibly more annotations to be considered: 
		//   http://answers.semanticweb.com/questions/298/how-can-you-represent-physical-addresses-in-foaf
		c.getAddress ();
		
		// ebi-terms:has-affiliation-line (subprop of dc:description/rdfs:comment)
		c.getAffiliation ();
		
		// rdfs:seeAlso
		c.getUrl ();
		
		// These need to be checked against Zooma
		c.getContactRoles ();
	}
}
