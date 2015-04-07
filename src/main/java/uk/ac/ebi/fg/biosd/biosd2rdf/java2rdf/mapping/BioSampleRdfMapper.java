package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils.urlEncode;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.Product;
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
import uk.ac.ebi.fg.java2rdf.utils.OwlApiUtils;

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
	@SuppressWarnings ( "rawtypes" )
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
		this.addPropertyMapper ( "databaseRecordRefs", new CollectionPropRdfMapper<> ( new CompositePropRdfMapper<> ( 
			new OwlObjPropRdfMapper<BioSample, DatabaseRecordRef> ( ns ( "pav", "derivedFrom" ) ),
			// dbrecord denotes submission
			new InversePropRdfMapper<BioSample, DatabaseRecordRef> ( 
				new OwlObjPropRdfMapper<DatabaseRecordRef, BioSample> ( ns ( "obo", "IAO_0000219" ) ) 
			)
		)));
		// TODO: more
	}

	@Override
	public boolean map ( BioSample smp, Map<String, Object> params )
	{
		if ( smp == null ) return false;
		
		// public/privacy status is only set for submissions, so, in order to be extra-sure it can be published, we
		// check all attached submissions. We also check at the sample level, just in case
		//
		Date releaseDate = smp.getReleaseDate ();
		if ( !Objects.equals ( false, smp.isPublic () ) || releaseDate != null && releaseDate.after ( new Date () ) )
		{
			log.trace ( "Skipping non-public sample '{}'", smp.getAcc () );
			return false;
		}
		for ( MSI msi: smp.getMSIs () )
			if ( !msi.isPublic () )
			{
				log.trace ( "Skipping non-public sample '{}'", smp.getAcc () );
				return false;
		}

		
		// Quite obviously, BioSD has no representation of a web page record, but we'd better add this, just in case
		// we want to show the external non-RDF records and provide with the corresponding links. 
		// The quickest way to achieve that is to send this Java object to its RDF mapper.
		DatabaseRecordRef biosdRef = new DatabaseRecordRef ( "EBI Biosamples Database", smp.getAcc (), null );
		biosdRef.setUrl ( "http://www.ebi.ac.uk/biosamples/sample/" + smp.getAcc () );
		
		smp.addDatabaseRecordRef ( biosdRef );

		// Add links coming from myEquivalents
		for ( DatabaseRecordRef dbxref: DbRecRefRdfMapper.getMyEquivalentsLinks ( "ebi.biosamples.samples", smp.getAcc () ) )
			smp.addDatabaseRecordRef ( dbxref );
		
		// Do you have a derivation property? These have to be treated specially
		//
		for ( Product<?> derivedFrom: smp.getDerivedFrom () )
			mapDerived ( smp, derivedFrom.getAcc (), true, params );			
		for ( Product<?> derivedTo: smp.getDerivedInto () )
			mapDerived ( smp, derivedTo.getAcc (), false, params );			
		
		boolean hasName = false;
		for ( ExperimentalPropertyValue<?> pval: smp.getPropertyValues () )
		{
			String pvalLabel = StringUtils.trimToNull ( pval.getTermText () );
			ExperimentalPropertyType ptype = pval.getType ();
			if ( pvalLabel == null ) continue;
			if ( ptype == null ) continue;
			String typeLabel = StringUtils.trimToNull ( ptype.getTermText () );
			if ( "Derived From".equalsIgnoreCase ( typeLabel ) )
				mapDerived ( smp, pvalLabel, true, params );			
			else if ( "Derived To".equalsIgnoreCase ( typeLabel ) ) 
				mapDerived ( smp, pvalLabel, false, params );
			else if ( "name".equalsIgnoreCase ( typeLabel ) || "sample name".equalsIgnoreCase ( typeLabel ) ) 
				hasName = true;
		}
		
		// Do you have a name? (names will be used for dcterms:title and rdfs:label
		// 
		if ( hasName ) return super.map ( smp, params ) | true;
		
		// You don't! Take the accession!
		BioCharacteristicType ntype = new BioCharacteristicType ( "name" );
		BioCharacteristicValue nval = new BioCharacteristicValue ( "Sample " + smp.getAcc (), ntype );
		smp.addPropertyValue ( nval );
		
		return super.map ( smp, params ) | true;
	}
	
	
	/**
	 * Add an assertion about the fact that the sample smp is derived from (or into ) the sample identified by otherAcc, 
	 * the direction from/into is established by isFrom.
	 * 
	 * This is used inside {@link #map(BioSample, Map)}.
	 * 
	 */
	private void mapDerived ( BioSample smp, String otherAcc, boolean isFrom, Map<String, Object> params )
	{
		RdfUriGenerator<BioSample> uriGen = this.getRdfUriGenerator ();
		OWLOntology kb = this.getMapperFactory ().getKnowledgeBase ();
		String luri = uriGen.getUri ( smp, params );
		// We prefer to reuse the URI generator, and this requires a sample
		String ruri = uriGen.getUri ( new BioSample ( otherAcc ), params );
		
		String sioDerivedFrom = ns ( "sio", "SIO_000244" ), sioDerivedInto = ns ( "sio", "SIO_000245" );
		String provDerivedFrom = ns ( "prov", "wasDerivedFrom" );
		
		if ( isFrom ) 
		{
			OwlApiUtils.assertLink ( kb, luri, sioDerivedFrom, ruri );
			OwlApiUtils.assertLink ( kb, luri, provDerivedFrom, ruri );
			OwlApiUtils.assertLink ( kb, ruri, sioDerivedInto, luri );
		}
		else
		{
			OwlApiUtils.assertLink ( kb, luri, sioDerivedInto, ruri );
			OwlApiUtils.assertLink ( kb, ruri, sioDerivedFrom, luri );
			OwlApiUtils.assertLink ( kb, ruri, provDerivedFrom, luri );
		}
	}
}

