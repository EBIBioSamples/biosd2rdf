package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;

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
					return ns ( "biosd", "sample/" + urlEncode ( smp.getAcc () ) );
			}}
		);
		this.addPropertyMapper ( "acc", new OwlDatatypePropRdfMapper<BioSample, String> ( ns ( "dc-terms", "identifier" ) ) );
		this.addPropertyMapper ( "propertyValues", new CollectionPropRdfMapper<BioSample, ExperimentalPropertyValue> ( 
			null, new ExpPropValueRdfMapper<BioSample> ()) 
		);
		// TODO: more
	}
}

