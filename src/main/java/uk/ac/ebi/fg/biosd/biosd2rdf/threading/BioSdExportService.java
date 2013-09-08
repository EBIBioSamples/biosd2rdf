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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManager;

import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.utils.memory.MemoryUtils;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>19 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdExportService
{
	private OWLOntology onto;
	private BioSdRfMapperFactory rdfMapFactory;

	private int threadPoolSize = 40;
	private ExecutorService executor = Executors.newFixedThreadPool ( threadPoolSize ); 

	private int busyTasks = 0;
	private long completedTasks = 0;
	private Lock submissionLock = new ReentrantLock ();
	private Condition freeTasksCond = submissionLock.newCondition (), noTasksCond = submissionLock.newCondition ();
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private int lastExitCode = 0;
	
	private String outputPath = null;
	private int outputCounter = 0;
	private Runnable memFlushAction = new Runnable() 
	{
		@Override
		public void run () {
			BioSdExportService.this.flushKnowledgeBase ();
		}
	};
	
	private PoolSizeTuner poolSizeTuner = null;

	public BioSdExportService ( String outputPath )
	{
		try
		{
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

	private void submit ( final BioSdExportTask exportTask )
	{
		// This will tune the thread pool size dynamically
		//
		if ( this.poolSizeTuner == null )
		{
			poolSizeTuner = new PoolSizeTuner () 
			{
				@Override
				protected void setThreadPoolSize ( int size ) 
				{
					submissionLock.lock ();
					threadPoolSize = size;
					((ThreadPoolExecutor) executor ).setCorePoolSize ( size );
					((ThreadPoolExecutor) executor ).setMaximumPoolSize ( size );
					submissionLock.unlock ();
				}
				
				@Override
				protected int getThreadPoolSize () {
					return threadPoolSize;
				}
				
				@Override
				protected long getCompletedTasks () {
					return completedTasks;
				}
			};
			poolSizeTuner.start ();
			
		} // poolSizeTuner check
		
		// This will flush the triples to the disk when the memory is too full
		MemoryUtils.checkMemory ( this.memFlushAction, 15d / 100d );
		// DEBUG if ( completedTasks > 0 && completedTasks % 20 == 0 ) flushKnowledgeBase ();
		
		submissionLock.lock ();
		try
		{
			// Wait until the pool has available threads
			while ( busyTasks >= threadPoolSize )
				try {
					freeTasksCond.await ();
				}
				catch ( InterruptedException ex ) {
					throw new RuntimeException ( "Internal error: " + ex.getMessage (), ex );
			}
			busyTasks++;
			log.info ( 
				"Submitted: " + exportTask.getMSI ().getAcc () + ", " + busyTasks + " task(s) running, " 
				+ completedTasks + " completed, please wait" 
			);
	
			// Now submit a new task, decorated with release code
			executor.submit ( new Runnable() 
			{
				@Override
				public void run ()
				{
					try
					{
						Thread.currentThread ().setName ( "Xport:" + exportTask.getMSI ().getAcc () );
						exportTask.run ();
					} 
					finally 
					{
						// Release after service run 
						submissionLock.lock ();
						try
						{
							int taskExitCode = exportTask.getExitCode ();
							if ( taskExitCode != 0 ) {
								if ( lastExitCode == 0 ) lastExitCode = taskExitCode; else if ( lastExitCode != taskExitCode ) lastExitCode = 1;
							}
							if ( --busyTasks < threadPoolSize ) 
							{
								freeTasksCond.signal ();
								if ( busyTasks == 0 ) noTasksCond.signalAll ();
							}
							completedTasks++;
							log.trace ( 
								Thread.currentThread ().getName () + " released, " + busyTasks + " task(s) running, " 
								+ completedTasks + ", completed" 
							);
						}
						finally {
							submissionLock.unlock ();
						}
						
					} // run().finally
				} // run()
			}); // decorated runnable
		} // try on submissionLock  
		finally {
			submissionLock.unlock ();
		}
	} // submit()

	
	public void submit ( Long msiId ) {
		submit ( new BioSdExportTask ( rdfMapFactory, msiId ) );
	}

	/** Used mainly for testing purposes. */
	public void submit ( MSI msi ) {
		submit ( new BioSdExportTask ( rdfMapFactory, msi ) );
	}


	@SuppressWarnings ( "unchecked" )
	public void submitAll ( double sampleSize )
	{
		EntityManager em = Resources.getInstance ().getEntityManagerFactory ().createEntityManager ();
		Random rnd = new Random ( System.currentTimeMillis () );
		sampleSize /= 100d;
		
		for ( Long id: (List<Long>) em.createQuery ( "SELECT id FROM MSI" ).getResultList () )
		{
			if ( rnd.nextDouble () >= sampleSize ) continue;
			submit ( id );
		}
	}

	public void waitAllFinished ()
	{
		Timer notificationTimer = new Timer ( "BioSdXport Alive Notification" );
		notificationTimer.scheduleAtFixedRate ( new TimerTask() {
			@Override
			public void run () {
				log.info ( "" + busyTasks + " task(s) still running, " + completedTasks + " completed, please wait" );
			}
		}, 60000, 60000 );

		submissionLock.lock ();
		try 
		{
			while ( this.busyTasks > 0 )
				noTasksCond.await ();
		}	
		catch ( InterruptedException ex ) {
			throw new RuntimeException ( "Internal error with multi-threading: " + ex.getMessage (), ex );
		}
		finally 
		{
			submissionLock.unlock ();
			notificationTimer.cancel ();
		}
	}
	
	
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
		
			// Don't measure/change the throughput while I'm idle
			if ( this.poolSizeTuner != null ) poolSizeTuner.stop ();
			
			String outp = null;
			if ( this.outputPath != null )
			{
				if ( outputCounter > 0 ) 
				{
					int idot = FilenameUtils.indexOfExtension ( this.outputPath );
					outp = idot == -1 
						? this.outputPath + "_" + outputCounter
						: this.outputPath.substring ( 0, idot ) + "_" + outputCounter + this.outputPath.substring ( idot );
				}
				else
					outp = this.outputPath;
				
				File fout = new File ( outp );
				log.info ( "Please wait, saving triples to '" + fout.getCanonicalPath () + "'" );
				kbout = new BufferedOutputStream ( new FileOutputStream ( outp ) );
			}
			else 
			{
				if ( outputCounter == 0 ) 
					log.info ( "Outputing in-memory tripes to the standard output" );
				else
					log.warn ( "Sending more than one OWL document to the standard output" );
				
				kbout = System.out;
				
			} // if outputPath
				
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
	
	
	@Override
	protected void finalize () throws Throwable
	{
		if ( poolSizeTuner != null ) this.poolSizeTuner.stop ();
		super.finalize ();
	}
}
