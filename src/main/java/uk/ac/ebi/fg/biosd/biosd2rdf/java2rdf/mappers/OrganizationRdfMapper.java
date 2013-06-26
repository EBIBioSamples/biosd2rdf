package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static org.apache.commons.lang.StringUtils.trimToNull;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.msi.Organization;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;

/**
 * TODO: Comment me!
 * 
 * TODO: submission dc-terms:contributor organization
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
			/* TODO:
			 * ebi-terms:OrganizationReference := 
  		 *   subClassOf ( 'is bearer of' (BFO_0000053) some 'role' (BFO_0000023) )
  		 *   subClassOf schema.org:Organization  dcterms:Agent, foaf:Organization
			 * 
			 */
			ns ( "ebi-terms", "OrganizationPerson" ), 
			new MSIEquippedRdfUriGenerator<Organization>()
			{
				@Override public String getUri ( Organization org ) 
				{
					String name = trimToNull ( org.getName () );
					if ( name == null ) return null;
					String id = trimToNull ( org.getEmail () );
					id = id != null ? hashUriSignature ( id ) : getMsiAcc () + "/" + name;    
					
					return ns ( "biosd", "organization/" + id );
			}}
		);
		
		Organization o = null;
		// schema.org properties, http://schema.rdfs.org/, 
		// also foaf properties
		o.getEmail ();
		// also dc:title/rdfs:comment
		o.getName ();

		// ebi-terms:has-address-line (subprop of dc:description/rdfs:comment)
		// TODO: possibly more annotations to be considered: 
		//   http://answers.semanticweb.com/questions/298/how-can-you-represent-physical-addresses-in-foaf
		o.getAddress ();
		
		// rdfs:seeAlso
		o.getURI ();
		
		// These need to be checked against Zooma
		o.getRole ();
	}
}
