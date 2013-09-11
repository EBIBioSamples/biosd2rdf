package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicType;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.java2rdf.mapping.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.properties.OwlObjPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mapping.urigen.RdfUriGenerator;

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
			// it's a subclass of iao:document_part and sio:collection too.
			ns ( "biosd-terms", "SampleGroup" ),
			// The MSI's accession is passed to the property value mapper
			new RdfUriGenerator<BioSampleGroup> () {
				@Override public String getUri ( BioSampleGroup sg, Map<String, Object> params ) {
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
	
	@Override
	public boolean map ( BioSampleGroup sg, Map<String, Object> params )
	{
		if ( sg == null ) return false;
		
		// Do you have a name? (names will be used for dcterms:title and rdfs:label
		// 
		for ( ExperimentalPropertyValue<?> pval: sg.getPropertyValues () ) 
		{
			String pvalLabel = StringUtils.trimToNull ( pval.getTermText () );
			ExperimentalPropertyType ptype = pval.getType ();
			if ( pvalLabel == null ) continue;
			if ( ptype == null ) continue;
			String typeLabel = StringUtils.trimToNull ( ptype.getTermText () );
			if ( "name".equalsIgnoreCase ( typeLabel ) || "Sample Name".equalsIgnoreCase ( typeLabel ) ) 
				return super.map ( sg, params );
		}

		// You don't! Take the accession!
		BioCharacteristicType ntype = new BioCharacteristicType ( "name" );
		BioCharacteristicValue nval = new BioCharacteristicValue ( "Sample " + sg.getAcc (), ntype );
		sg.addPropertyValue ( nval );
		
		return super.map ( sg, params );
	}
	
}

