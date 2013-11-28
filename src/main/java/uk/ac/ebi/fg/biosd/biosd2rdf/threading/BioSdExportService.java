package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.getNamespaces;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.biosd2rdf.utils.XmlCharFixer;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.utils.memory.MemoryUtils;
import uk.ac.ebi.utils.threading.BatchService;

/**
 * An extension of {@link BatchService} to run multiple BioSD/RDF exporters in parallel, in multi-threading fashion.
 * Here we keep a single OWL triple store, where all the threads pour their output and we flush it to a (numbered) file, 
 * when we see that we are running out of RAM.
 * 
 * The parallelisation is realised on a per-submission basis, i.e. each {@link BioSdExportTask} is a thread that exports
 * everything contained in a SampleTab submission.
 *
 * <dl><dt>date</dt><dd>19 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdExportService extends BatchService<BioSdExportTask>
{
	private OWLOntology onto;
	private BioSdRfMapperFactory rdfMapFactory;
		
	private String outputPath = null;
	private int outputCounter = 0;
	private Runnable memFlushAction = new Runnable() 
	{
		@Override
		public void run () {
			BioSdExportService.this.flushKnowledgeBase ();
		}
	};

	/**
	 * This sets up proper parameters for {@link #getPoolSizeTuner()} and initialises the {@link #onto triple store} used
	 * to save the exporters output.
	 * 
	 * outputPath is where the path of the output file to save. This will be properly numbered in case multiple files are
	 * needed, due to memory limitations. For instance, if you send in /tmp/test.owl, you might get back 
	 * /tmp/test1.owl, /tmp/test2.owl ecc. If this parameter is null, all the output is poured to the standard output, 
	 * and this will likely not be a single RDF document (so it's not recommended that you use this approach).
	 * 
	 */
	public BioSdExportService ( String outputPath )
	{
		super ( Runtime.getRuntime().availableProcessors() );
		// super ( Runtime.getRuntime().availableProcessors(), null ); // DEBUG
		try
		{
			// Sometimes I set it to null for debugging purposes
			if ( this.poolSizeTuner != null ) 
			{
				this.poolSizeTuner.setPeriodMSecs ( (int) 5*60*1000 );
				this.poolSizeTuner.setMaxThreads ( 100 );
				this.poolSizeTuner.setMinThreads ( 5 );
				this.poolSizeTuner.setMaxThreadIncr ( 25 );
				this.poolSizeTuner.setMinThreadIncr ( 5 );
			}
			
			this.outputPath = outputPath;
			
			BioSdRfMapperFactory.init (); // cause the definition of BioSD-specific namespaces
			OWLOntologyManager owlMgr = OWLManager.createOWLOntologyManager ();
			onto = owlMgr.createOntology ( IRI.create ( ns ( "biosd-dataset" ) ) );
			rdfMapFactory = new BioSdRfMapperFactory ( onto );
		} 
		catch ( OWLOntologyCreationException ex ) {
			throw new RuntimeException ( "Internal error with OWL-API: " + ex.getMessage (), ex );
		}
	}
	
	/** 
	 * Submits the task of exporting a single SampleTab submission to RDF, of which the database ID is known. This is used by 
	 * {@link #submitAll(double)} and wraps {@link #submit(BioSdExportTask)}.  
	 * 
	 */
	public void submit ( Long msiId ) {
		this.submit ( new BioSdExportTask ( rdfMapFactory, msiId ) );
	}
	
	public void submit ( String msiAcc ) {
		this.submit ( new BioSdExportTask ( rdfMapFactory, msiAcc ) );
	}

	/** 
	 * Submits the task of exporting a single SampleTab to RDF submission, available as {@link MSI MSI object}. 
	 * This is used mainly for testing purposes and wraps {@link #submit(BioSdExportTask)}. 
	 */
	public void submit ( MSI msi ) {
		this.submit ( new BioSdExportTask ( rdfMapFactory, msi ) );
	}

	
	
	/**
	 * Submits a new BioSD exporter task. This will work on a single SampleTab submission and will be run as a thread in 
	 * thread pool, managed by this service.
	 */
	@Override
	public void submit ( BioSdExportTask batchServiceTask )
	{
		// This will flush the triples to the disk when the memory is too full and will also invoke the GC
		MemoryUtils.checkMemory ( this.memFlushAction, 15d / 100d );
		// DEBUG if ( completedTasks > 0 && completedTasks % 20 == 0 ) flushKnowledgeBase ();

		super.submit ( batchServiceTask );
	}

	/**
	 * Submits all the samples in the BioSD relational database (configured via hibernate.properties), or a random sample, 
	 * represented by the method parameter (which ranges from 0 to 100).
	 */
	@SuppressWarnings ( "unchecked" )
	public void submitAll ( double sampleSize )
	{
		EntityManager em = Resources.getInstance ().getEntityManagerFactory ().createEntityManager ();
		Random rnd = new Random ( System.currentTimeMillis () );
		sampleSize /= 100d;
		
		Query q = em.createQuery ( "SELECT id FROM MSI" ).setHint ( "org.hibernate.readOnly", true );
		for ( Long id: (List<Long>) q.getResultList () )
		{
			if ( rnd.nextDouble () >= sampleSize ) continue;
			submit ( id );
		}
	}

	/**
	 * Exports the SampleTab submissions having the parameter accessions. Used in the corresponding form of 
	 * the command line, uses {@link BioSdExportTask#BioSdExportTask(BioSdRfMapperFactory, String)}.  
	 */
	public void submitAll ( String... msiAccs )
	{
		for ( String msiAcc: msiAccs ) submit ( msiAcc );
	}
	

	/**
	 * This saves the triple store that keeps the RDF output of the exporters into the file specified by this class's 
	 * constructor. This method should be called at the end of a sequence of submissions, after {@link #waitAllFinished()}
	 * returns. 
	 * 
	 * It is also invoked when the RAM available to the JVM executing this service is running out and multiple
	 * calls of this method will generate numbered file names (see {@link BioSdExportService}). Moreover, each invocation
	 * empties the triple store at issue and allows the JVM free its memory for new triples. 
	 * 
	 */
	public void flushKnowledgeBase ()
	{
		// This is the only way to avoid that multiple threads access a KB that needs to be replaced. I've experienced
		// that synchronisation over this.onto is not enough.
		//
		log.info ( "Waiting all tasks to finish, before flusing the RDF triples created so far to stdout/file" );
		waitAllFinished ();

		OutputStream kbout = null;
				
		try
		{			
			if ( this.onto.isEmpty () ) return; 
		
			// Don't measure/change the throughput while I'm not actually servicing tasks
			if ( this.poolSizeTuner != null ) poolSizeTuner.stop ();
			
			String outp = null;
			// Do we really have an output path?
			if ( this.outputPath != null )
			{
				if ( outputCounter > 0 ) 
				{
					// If it's not the first time, number the file between the name and the extension.
					int idot = FilenameUtils.indexOfExtension ( this.outputPath );
					outp = idot == -1 
						? this.outputPath + "_" + outputCounter
						: this.outputPath.substring ( 0, idot ) + "_" + outputCounter + this.outputPath.substring ( idot );
				}
				else
					outp = this.outputPath;
				
				File fout = new File ( outp );
				log.info ( "Please wait, saving triples to '" + fout.getCanonicalPath () + "'" );
				kbout = new BufferedOutputStream ( new XmlCharFixer ( new FileOutputStream ( outp ) ) );
			}
			else 
			{
				// no output path specified, I'm going to punish you by vomiting all on your screen...
				if ( outputCounter == 0 ) 
					log.info ( "Outputing in-memory tripes to the standard output" );
				else
					log.warn ( "Sending more than one OWL document to the standard output" );
				
				kbout = System.out;
				
			} // if outputPath
				
			// RDF/XML output. TODO: make this an option?
			// 
			PrefixOWLOntologyFormat fmt = new RDFXMLOntologyFormat ();
			for ( Entry<String, String> nse: getNamespaces ().entrySet () )
				fmt.setPrefix ( nse.getKey (), nse.getValue () );
			onto.getOWLOntologyManager ().saveOntology ( this.onto, fmt, kbout );
			
			OWLOntologyManager owlMgr = OWLManager.createOWLOntologyManager ();
			this.onto = owlMgr.createOntology ( IRI.create ( ns ( "biosd-dataset" ) ) );
			this.rdfMapFactory.setKnowledgeBase ( onto );
		} 
		catch ( IOException|OWLOntologyStorageException|OWLOntologyCreationException ex ) {
			throw new IllegalArgumentException ( "Error while saving exported triples: " + ex.getMessage (), ex );
		}
		finally 
		{
			this.outputCounter++;
			
			// Restart dynamic thread pool size optimisation if it was stopped
			if ( kbout != null && this.poolSizeTuner != null ) poolSizeTuner.start ();
		}
	} // flushKnowledgeBase
	
}
