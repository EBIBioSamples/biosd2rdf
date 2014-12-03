package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.biosd2rdf.Biosd2RdfCmd;
import uk.ac.ebi.fg.biosd.model.utils.test.TestModel;
import uk.ac.ebi.fg.biosd.sampletab.persistence.Persister;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;

/**
 * Basic tests of the BioSD exporter.
 *
 * <dl><dt>date</dt><dd>7 Sep 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class ExporterTest
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	/**
	 * Loads mock-up SampleTab submissions and then export them via the multi-threaded exporter.
	 */
	@Test 
	public void testExporter ()
	{
		Persister persister = new Persister ();
		
		for ( long id = 1; id <= 50; id++ )
		{
			TestModel tm = new TestModel ( "test." + id + "." );

			// We currently set this at submission level only
			tm.msi.setPublicFlag ( true );
			
			persister.persist ( tm.msi );
			log.info ( "{} persisted", tm.msi.getAcc () );
		}
		
		System.setProperty ( "biosd.test_mode", "true" );
		Biosd2RdfCmd.main ( "-o", "target/biosd_test.ttl" );
	}
	
	/**
	 * Creates mock-up SampleTab submissions and export them, without involving any relational DB.
	 */
	@Test 
	public void testNoDbExport ()
	{
		BioSdExportService xservice = new BioSdExportService ( "target/biosd_nodb_test.ttl" );
		
		for ( long id = 1; id <= 50; id++ )
		{
			TestModel tm = new TestModel ( "test." + id + "." );

			// We currently set this at submission level only
			tm.msi.setPublicFlag ( true );

			
			tm.cv3.addOntologyTerm ( new OntologyEntry ( "FOO ACC", new ReferenceSource ( "FOO ONTO", null ) ) );
			log.debug ( "Exporting {}", tm.msi.getAcc () );
			xservice.submit ( tm.msi );
		}
		
		xservice.waitAllFinished ();
		xservice.flushKnowledgeBase ();
	}

	/**
	 * Exports whatever it finds in the POM-configured DB
	 */
	@Test @Ignore ( "Not a real test" )
	public void testExporterFromExistingDb ()
	{
		System.setProperty ( "biosd.test_mode", "true" );
		Biosd2RdfCmd.main ( "-o", "target/biosd_realdb_test.ttl" );
	}

}
