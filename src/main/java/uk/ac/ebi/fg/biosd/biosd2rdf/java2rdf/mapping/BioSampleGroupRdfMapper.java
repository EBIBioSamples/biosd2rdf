package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
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

/**
 * Maps a BioSD Sample Group to RDF.
 *
 * <dl><dt>date</dt><dd>Apr 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSampleGroupRdfMapper extends BeanRdfMapper<BioSampleGroup>
{
	@SuppressWarnings ( "rawtypes" )
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
		this.addPropertyMapper ( "samples", new CollectionPropRdfMapper<BioSampleGroup, BioSample> (
			new OwlObjPropRdfMapper<BioSampleGroup, BioSample> ( ns ( "sio", "SIO_000059" ) ) // has member
		));
		this.addPropertyMapper ( "databaseRecordRefs", new CollectionPropRdfMapper<> ( new CompositePropRdfMapper<> ( 
			new OwlObjPropRdfMapper<BioSampleGroup, DatabaseRecordRef> ( ns ( "pav", "derivedFrom" ) ),
			// dbrecord denotes submission
			new InversePropRdfMapper<BioSampleGroup, DatabaseRecordRef> ( 
				new OwlObjPropRdfMapper<DatabaseRecordRef, BioSampleGroup> ( ns ( "obo", "IAO_0000219" ) ) 
			)
		)));
		
		// TODO: more
	}
	
	@Override
	public boolean map ( BioSampleGroup sg, Map<String, Object> params )
	{
		if ( sg == null ) return false;

		// public/privacy status is only set for submissions, so, in order to be extra-sure it can be published, we
		// check all attached submissions. We also check at the sample level, just in case
		//
		Date releaseDate = sg.getReleaseDate ();
		if ( !Objects.equals ( false, sg.isPublic () ) || releaseDate != null && releaseDate.after ( new Date () ) )
		{
			log.trace ( "Skipping non-public sample group '{}'", sg.getAcc () );
			return false;
		}
		for ( MSI msi: sg.getMSIs () )
			if ( !msi.isPublic () )
			{
				log.trace ( "Skipping non-public sample group '{}'", sg.getAcc () );
				return false;
		}
		
		// Quite obviously, BioSD has no representation of a web page record, but we'd better add this, just in case
		// we want to show the external non-RDF records and provide with the corresponding links. 
		// The quickest way to achieve that is to send this Java object to its RDF mapper.
		DatabaseRecordRef biosdRef = new DatabaseRecordRef ( "EBI Biosamples Database", sg.getAcc (), null );
		biosdRef.setUrl ( "http://www.ebi.ac.uk/biosamples/group/" + sg.getAcc () );

		sg.addDatabaseRecordRef ( biosdRef );
		
		// Map this manually, we noticed it doesn't work if we just add it to the sample.
		// TODO REMOVE dbRefMapper.map ( sg, Collections.singleton ( biosdRef ) );
		
		// Do you have a name? (names will be used for dcterms:title and rdfs:label
		// 
		for ( ExperimentalPropertyValue<?> pval: sg.getPropertyValues () ) 
		{
			String pvalLabel = StringUtils.trimToNull ( pval.getTermText () );
			ExperimentalPropertyType ptype = pval.getType ();
			if ( pvalLabel == null ) continue;
			if ( ptype == null ) continue;
			String typeLabel = StringUtils.trimToNull ( ptype.getTermText () );
			if ( typeLabel != null && typeLabel.toLowerCase ().matches ( "(sample |group |sample group |)?name" ) ) 
				return super.map ( sg, params ) | true;
		}

		// You don't! Take the accession!
		BioCharacteristicType ntype = new BioCharacteristicType ( "name" );
		BioCharacteristicValue nval = new BioCharacteristicValue ( "Sample Group " + sg.getAcc (), ntype );
		sg.addPropertyValue ( nval );
		
		return super.map ( sg, params ) | true;
	}
	
}

