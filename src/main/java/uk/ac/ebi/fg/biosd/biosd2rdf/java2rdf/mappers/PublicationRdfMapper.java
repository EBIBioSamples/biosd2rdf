package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.mappers.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils;
import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Jun 18, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class PublicationRdfMapper extends BeanRdfMapper<Publication>
{
	public PublicationRdfMapper ()
	{
		super (
			ns ( "obo", "IAO_0000311" ), // publication TODO: foaf:document, fabio:work
			new RdfUriGenerator<Publication>() {
				@Override public String getUri ( Publication pub ) 
				{
					String title = StringUtils.trimToNull ( pub.getTitle () ); if ( title == null ) return null;
					String authorList = StringUtils.trimToNull ( pub.getAuthorList () ); if ( authorList == null ) return null;
					
					String pmid = urlEncode ( StringUtils.trimToNull ( pub.getPubmedId () ) );
					
					String id;
					if ( pmid != null ) 
						id = pmid;
					else 
					{
						String doi = urlEncode ( StringUtils.trimToNull ( pub.getDOI () ) );
						if ( doi != null )
							id = Java2RdfUtils.hashUriSignature ( doi );
						else 
						{
							int year = NumberUtils.toInt ( pub.getYear (), -1 );
							id = Java2RdfUtils.hashUriSignature ( title + authorList +  ( year == -1 ? "" : year ) );
						}
					}
					return ns ( "biosd", "publication/" + id );
			}}
		);
		
		this.addPropertyMapper ( "title", new OwlDatatypePropRdfMapper<Publication, String> ( ns ( "dc-terms", "title" ) ) );
		// TODO: add EDAM identifiers, which are individuals and not a data values.
		this.addPropertyMapper ( "pubmedId", new OwlDatatypePropRdfMapper<Publication, String> ( ns ( "fabio", "hasPubMedId" ) ) );
		this.addPropertyMapper ( "DOI", new OwlDatatypePropRdfMapper<Publication, String> ( ns ( "prism", "doi" ) ) );
		// TODO: a sub-property of dc-elems:creator and of owl:dataProperty
		this.addPropertyMapper ( "authorList", new OwlDatatypePropRdfMapper<Publication, String> ( ns ( "ebi-terms", "has-authors-list" ) ) );
		this.addPropertyMapper ( "year", new PublicationYearRdfMapper () );
	}
}
