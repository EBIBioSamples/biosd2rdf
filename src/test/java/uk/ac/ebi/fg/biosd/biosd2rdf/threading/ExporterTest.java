package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.biosd2rdf.Biosd2RdfCmd;
import uk.ac.ebi.fg.biosd.model.utils.test.TestModel;
import uk.ac.ebi.fg.biosd.sampletab.persistence.Persister;

/**
 * TODO: Comment me!
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
	@Test @Ignore ( "Too time-consuming, used for manual tests" )
	public void testExporter ()
	{
		Persister persister = new Persister ();
		
		for ( long id = 1; id <= 100; id++ )
		{
			TestModel tm = new TestModel ( "test." + id + "." );
			persister.persist ( tm.msi );
			log.debug ( "{} persisted", tm.msi.getAcc () );
		}
		
		Biosd2RdfCmd.main ( "-o", "target/biosd_test.owl" );
	}
	
	/**
	 * Creates mock-up SampleTab submissions and export them, without involving any relational DB.
	 */
	@Test
	public void testNoDbExport ()
	{
		BioSdExportService xservice = new BioSdExportService ( "target/biosd_nodb_test.owl" );
		
		for ( long id = 1; id <= 100; id++ )
		{
			TestModel tm = new TestModel ( "test." + id + "." );
			log.debug ( "Exporting {}", tm.msi.getAcc () );
			xservice.submit ( tm.msi );
		}
		
		xservice.waitAllFinished ();
		xservice.flushKnowledgeBase ();
	}

}
