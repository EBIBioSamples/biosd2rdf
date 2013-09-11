package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static org.apache.commons.lang.StringUtils.trimToNull;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.hashUriSignature;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.HashMap;
import java.util.Map;

import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CompositePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;

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
  		 *   ebi-terms:Organization := subClassOf ( schema.org:Organization, dcterms:Agent, foaf:Organization )
			 * 
			 */
			ns ( "biosd-terms", "ContactOrganization" ), 
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
					
					return ns ( "biosd", "organization/" + id );
			}}
		);
		
		// TODO: schema.org properties, http://schema.rdfs.org/, 
		// also foaf properties

		this.addPropertyMapper ( "email", new OwlDatatypePropRdfMapper<Organization, String> ( ns ( "sch", "email" ) ) );
		this.addPropertyMapper ( "name", new CompositePropRdfMapper<> ( 
			new OwlDatatypePropRdfMapper<Organization, String> ( ns ( "sch", "name" ) ),
			new OwlDatatypePropRdfMapper<Organization, String> ( ns ( "dc-terms", "title" ) ),
			new OwlDatatypePropRdfMapper<Organization, String> ( ns ( "rdfs", "label" ) )
		));
		this.addPropertyMapper ( "description", new CompositePropRdfMapper<> (
			new OwlDatatypePropRdfMapper<Organization, String> ( ns ( "sch", "description" ) ),
			new OwlDatatypePropRdfMapper<Organization, String> ( ns ( "dc-terms", "description" ) ),
			new OwlDatatypePropRdfMapper<Organization, String> ( ns ( "rdfs", "comment" ) ) 
		));

		
		// TODO: also dc-terms:title/rdfs:label
		// TODO: not possible for the moment, requires the ability to associate multiple mappers to the bean property: 
		// this.setPropertyMapper ( new OwlDatatypePropRdfMapper<Organization, String> ( "name", ns ( "dc-terms", "title" ) ) );

		// TODO: ebi-terms:has-address-line (subprop of dc:description/rdfs:comment)
		// TODO: possibly more annotations to be considered: 
		//   http://answers.semanticweb.com/questions/298/how-can-you-represent-physical-addresses-in-foaf
		this.addPropertyMapper ( "address", new OwlDatatypePropRdfMapper<Organization, String> ( ns ( "biosd-terms", "has-address-line" ) ) );
		
		// TODO: requires a special mapper that goes from string to URIs, without involving bean classes or URI generators
		// this.setPropertyMapper ( new OwlObjPropRdfMapper<Organization, String> ( "URI", ns ( "rdfs", "seeAlso" ) ) );
		
		// TODO: These need to be checked against Zooma
		// o.getRole ();
	}
}
