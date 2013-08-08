package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlObjPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;

/**
 * Maps a BioSD Sample Group to RDF.
 *
 * <dl><dt>date</dt><dd>Apr 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSampleGroupRdfMapper extends BeanRdfMapper<BioSampleGroup>
{
	public BioSampleGroupRdfMapper ()
	{
		super ( 
			// TODO: we need to subclass this from iao:document_part (and maybe a sio:collection too)
			ns ( "biosd-term", "SampleGroup" ), 
			new RdfUriGenerator<BioSampleGroup> () {
				@Override public String getUri ( BioSampleGroup sg ) {
					return ns ( "biosd", "sample-group/" + urlEncode ( sg.getAcc () ) );
			}});
		this.addPropertyMapper ( "acc", new OwlDatatypePropRdfMapper<BioSampleGroup, String> ( ns ( "dc-terms", "identifier" ) ) );
		this.addPropertyMapper ( "propertyValues", new CollectionPropRdfMapper<BioSampleGroup, ExperimentalPropertyValue> ( 
			new ExpPropValueRdfMapper<BioSampleGroup> ()) 
		);
		// is_about, TODO: probably needs 'sio:has member' too.
		this.addPropertyMapper ( "samples", new CollectionPropRdfMapper<BioSampleGroup, BioSample> ( 
			new OwlObjPropRdfMapper<BioSampleGroup, BioSample> ( ns ( "obo", "IAO_0000136" ) ) )
		);
		// TODO: more
	}
}

