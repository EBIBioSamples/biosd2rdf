package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.uri;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CompositePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils;

/**
 * Maps a publication that is associated to a BioSD/SampleTab submission to OWL/RDF.
 * 
 * TODO: we create minimal properties for each publication (title, author list and few else), at the moment we don't
 * produce even these, since all BioSD records only have PUBMED-ID or DOI. For such records, we still generate links
 * to publication RDF resources and attach the identifiers to them. This way, the resulting RDF can be used to explore
 * more information about publications, by means of federated queries against resources like 
 * <a href = 'http://bio2rdf.org/'>Bio2RDF</a> or <a href = 'http://linkedlifedata.com/'>LLD</a>.
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
			// publication, ebi_terms.owl additionally defines this as subclass of: foaf:document, fabio:work, bibo:Document
			uri ( "obo", "IAO_0000311" ), 
			new RdfUriGenerator<Publication>() {
				@Override public String getUri ( Publication pub, Map<String, Object> params ) 
				{
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
							String title = StringUtils.trimToNull ( pub.getTitle () ); if ( title == null ) return null;
							String authorList = StringUtils.trimToNull ( pub.getAuthorList () ); if ( authorList == null ) return null;
							int year = NumberUtils.toInt ( pub.getYear (), -1 );
							id = Java2RdfUtils.hashUriSignature ( title + authorList +  ( year == -1 ? "" : year ) );
						}
					}
					return uri ( "biosd", "publication/" + id );
			}}
		);
		
		this.addPropertyMapper ( "title", new CompositePropRdfMapper<> (  
			new OwlDatatypePropRdfMapper<Publication, String> ( uri ( "dc-terms", "title" ) ), 
			new OwlDatatypePropRdfMapper<Publication, String> ( uri ( "rdfs", "label" ) ) 
		));

		
		// TODO: add EDAM identifiers, which are individuals and not a data values.
		this.addPropertyMapper ( "pubmedId", new CompositePropRdfMapper<> ( 
			new OwlDatatypePropRdfMapper<Publication, String> ( uri ( "fabio", "hasPubMedId" ) ), 
			new OwlDatatypePropRdfMapper<Publication, String> ( uri ( "bibo", "pmid" ) ) 
		));
		this.addPropertyMapper ( "DOI", new CompositePropRdfMapper<> ( 
			new OwlDatatypePropRdfMapper<Publication, String> ( uri ( "bibo", "doi" ) ), 
			new OwlDatatypePropRdfMapper<Publication, String> ( uri ( "prism", "doi" ) ) 
		));
		// TODO: a sub-property of dc-elems:creator and of owl:dataProperty
		this.addPropertyMapper ( "authorList", new OwlDatatypePropRdfMapper<Publication, String> ( uri ( "biosd-terms", "has-authors-list" ) ) );
		this.addPropertyMapper ( "year", new PublicationYearRdfMapper () );
	}
}
