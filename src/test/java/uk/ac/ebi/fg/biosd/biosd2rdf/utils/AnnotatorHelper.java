package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import java.util.Date;

import org.joda.time.DateTime;

import uk.ac.ebi.fg.biosd.annotator.AnnotatorResources;
import uk.ac.ebi.fg.biosd.annotator.PropertyValAnnotationManager;
import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorPersister;
import uk.ac.ebi.fg.biosd.annotator.purge.Purger;
import uk.ac.ebi.fg.biosd.annotator.threading.PropertyValAnnotationService;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;

/**
 * An helper to generate annotations via the 
 * <a href='https://github.com/EBIBioSamples/biosd_feature_annotator'>BioSD feature annotator</a>, before translating 
 * them into triples, using the RDF exporting functions.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>20 Jan 2016</dd></dl>
 *
 */
public class AnnotatorHelper
{
	public void annotate ( MSI msi )
	{
		for ( BioSample smp: msi.getSamples () )
			annotate ( smp );

		for ( BioSampleGroup sg: msi.getSampleGroups () )
			annotate ( sg );
	}
	
	@SuppressWarnings ( "unchecked" )
	public void annotate ( BioSample smp )
	{
		for ( ExperimentalPropertyValue<ExperimentalPropertyType> pv: smp.getPropertyValues () )
			annotate ( pv );
	}
	
	@SuppressWarnings ( "unchecked" )
	public void annotate ( BioSampleGroup sg )
	{
		for ( ExperimentalPropertyValue<ExperimentalPropertyType> pv: sg.getPropertyValues () )
			annotate ( pv );
	}
	
	public void annotate ( ExperimentalPropertyValue<ExperimentalPropertyType> pv )
	{
		PropertyValAnnotationManager annMgr = AnnotatorResources.getInstance ().getPvAnnMgr ();
		annMgr.annotate ( pv );
	}
	
	public void begin ()
	{
		Purger purger = new Purger ();
		purger.purge ( new DateTime ().minusMinutes ( 5 ).toDate (), new Date () );
		AnnotatorResources.getInstance ().reset ();
	}

	
	public long commit ()
	{
		long result = commitNoReset ();
		AnnotatorResources.getInstance ().reset ();
		return result;
	}

	public long commitNoReset ()
	{
		return new AnnotatorPersister ().persist ();
	}

	public void annotateAll ()
	{
		begin ();
		PropertyValAnnotationService annService = new PropertyValAnnotationService ();
		annService.submitAll ();
		annService.waitAllFinished ();
		AnnotatorResources.getInstance ().reset ();
	}
}
