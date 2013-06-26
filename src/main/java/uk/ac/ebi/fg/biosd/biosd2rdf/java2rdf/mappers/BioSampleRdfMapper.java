package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.mappers.ToDatatypePropRdfMapper;

/**
 * Maps a BioSD sample to RDF.
 *
 * <dl><dt>date</dt><dd>Apr 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSampleRdfMapper extends BeanRdfMapper<BioSample>
{
	public BioSampleRdfMapper ()
	{
		super ( 
			// material sample (TODO: obi:material entity? Samples are collected and stored)
			ns ( "obo", "OBI_0000747" ), 
			new RdfUriGenerator<BioSample> () {
				@Override public String getUri ( BioSample smp ) {
					return ns ( "biosd", "sample/" + smp.getAcc () );
			}}
		);
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<BioSample, String> ( "acc", ns ( "dc-terms", "identifier" ) ) );
		this.setPropertyMapper ( new CollectionPropRdfMapper<BioSample, ExperimentalPropertyValue> ( 
			"propertyValues", null, new ExpPropValueRdfMapper<BioSample> ()) 
		);
		// TODO: more
	}
}

