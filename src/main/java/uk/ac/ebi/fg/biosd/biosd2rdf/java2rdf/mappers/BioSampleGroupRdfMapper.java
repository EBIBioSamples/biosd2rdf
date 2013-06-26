package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.mappers.ToDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.ToObjectPropRdfMapper;

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
					return ns ( "biosd", "sample-group/" + sg.getAcc () );
			}});
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<BioSampleGroup, String> ( "acc", ns ( "dc-terms", "identifier" ) ) );
		this.setPropertyMapper ( new CollectionPropRdfMapper<BioSampleGroup, ExperimentalPropertyValue> ( 
			"propertyValues", null, new ExpPropValueRdfMapper<BioSampleGroup> ()) 
		);
		// is_about, TODO: probably needs 'sio:has member' too.
		this.setPropertyMapper ( new CollectionPropRdfMapper<BioSampleGroup, BioSample> ( 
			"samples", ns ( "obo", "IAO_0000136" ), new ToObjectPropRdfMapper<BioSampleGroup, BioSample> () ) {}
		);
		// TODO: more
	}
}

