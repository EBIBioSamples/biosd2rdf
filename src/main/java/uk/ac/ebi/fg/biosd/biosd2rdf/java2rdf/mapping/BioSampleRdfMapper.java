package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicType;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CompositePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.InversePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlObjPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;

import java.util.Map;

/**
 * Maps a BioSD sample to RDF. <a href = 'http://www.ebi.ac.uk/rdf/documentation/biosamples'>Here</a> you can find 
 * examples of what this class produces. 
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
			// subclass of material sample (TODO: obi:material entity? Samples are supposed to be collected and stored)
			ns ( "biosd-terms", "Sample" ), 
			// The MSI's accession is passed to the property value mapper
			new RdfUriGenerator<BioSample> () {
				@Override public String getUri ( BioSample smp, Map<String, Object> params ) {
					return ns ( "biosd", "sample/" + urlEncode ( smp.getAcc () ) );
			}}
		);
		this.addPropertyMapper ( "acc", new OwlDatatypePropRdfMapper<BioSample, String> ( ns ( "dc-terms", "identifier" ) ) );
		this.addPropertyMapper ( "propertyValues", new CollectionPropRdfMapper<BioSample, ExperimentalPropertyValue> ( 
			new ExpPropValueRdfMapper<BioSample> ()) 
		);
		this.addPropertyMapper ( "databases", new CompositePropRdfMapper<BioSample, DatabaseRefSource> ( 
			new OwlObjPropRdfMapper<BioSample, DatabaseRefSource> ( ns ( "pav", "derivedFrom" ) ),
			// dbrecord denotes sample
			new InversePropRdfMapper<BioSample, DatabaseRefSource> ( 
				new OwlObjPropRdfMapper<DatabaseRefSource, BioSample> ( ns ( "obo", "IAO_0000219" ) ) 
			)
		));
		// TODO: more
	}

	@Override
	public boolean map ( BioSample smp, Map<String, Object> params )
	{
		if ( smp == null ) return false;
		
		// Quite obviously, BioSD has no representation of a web page record, but we'd better add this, just in case
		// we want to show the external non-RDF records and provide with the corresponding links. 
		// The quickest way to achieve that is to send this Java object to its RDF mapper.
		DatabaseRefSource biosdRef = new DatabaseRefSource ( smp.getAcc (), null );
		biosdRef.setName ( "EBI Biosamples Database" );
		biosdRef.setUrl ( "http://www.ebi.ac.uk/biosamples/sample/" + smp.getAcc () );
		
		// Do you have a name? (names will be used for dcterms:title and rdfs:label
		// 
		for ( ExperimentalPropertyValue<?> pval: smp.getPropertyValues () ) 
		{
			String pvalLabel = StringUtils.trimToNull ( pval.getTermText () );
			ExperimentalPropertyType ptype = pval.getType ();
			if ( pvalLabel == null ) continue;
			if ( ptype == null ) continue;
			String typeLabel = StringUtils.trimToNull ( ptype.getTermText () );
			if ( "name".equalsIgnoreCase ( typeLabel ) || "sample name".equalsIgnoreCase ( typeLabel ) ) 
				return super.map ( smp, params );
		}

		// You don't! Take the accession!
		BioCharacteristicType ntype = new BioCharacteristicType ( "name" );
		BioCharacteristicValue nval = new BioCharacteristicValue ( "Sample " + smp.getAcc (), ntype );
		smp.addPropertyValue ( nval );
		
		return super.map ( smp, params );
	}
	
	
}

