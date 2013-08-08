package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.util.List;
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

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.core_model.resources.Resources;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>19 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdExportService
{
	private OWLOntologyManager owlMgr;
	private OWLOntology onto;
	private BioSdRfMapperFactory rdfMapFactory;

	private int threadPoolSize = 25;
	private ExecutorService executor = Executors.newFixedThreadPool ( threadPoolSize ); 

	private int busyTasks = 0;
	private long completedTasks = 0;
	private Lock submissionLock = new ReentrantLock ();
	private Condition freeTasksCond = submissionLock.newCondition (), noTasksCond = submissionLock.newCondition ();
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private int lastExitCode = 0;
	
	private PoolSizeTuningTimerTask poolSizeTunerTimerTask = null;

	public BioSdExportService ()
	{
		try
		{
			BioSdRfMapperFactory.init (); // cause the definition of BioSD-specific namespaces
			owlMgr = OWLManager.createOWLOntologyManager ();
			onto = owlMgr.createOntology ( IRI.create ( ns ( "biosd-dataset" ) ) );
			rdfMapFactory = new BioSdRfMapperFactory ( onto );
		} 
		catch ( OWLOntologyCreationException ex ) {
			throw new RuntimeException ( "Internal error with OWL-API: " + ex.getMessage (), ex );
		}
	}

	private void submit ( final BioSdExportTask exportTask )
	{
		// This will dynamically tune the thread pool size
		//
		if ( this.poolSizeTunerTimerTask == null )
		{
			poolSizeTunerTimerTask = new PoolSizeTuningTimerTask () 
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

			poolSizeTunerTimerTask.start ();
		}
		
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
							submissionLock.notifyAll ();
						}
						finally {
							submissionLock.unlock ();
						}
						
					} // run().finally
				} // run()
			}); // decorated runnable
			
			busyTasks++;
			log.info ( 
				"Submitted: " + exportTask.getMSI ().getAcc () + ", " + busyTasks + " task(s) running, " 
				+ completedTasks + " completed, please wait" 
			);
			
		} // service submissionLock.lock()
		finally {
			submissionLock.unlock ();
		}
	} // submit()

	
	public void submit ( Long msiId ) {
		submit ( new BioSdExportTask ( rdfMapFactory, msiId ) );
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
				log.info ( "" + busyTasks + " expoter(s) still running, " + completedTasks + " completed, please wait" );
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
	
	public OWLOntology getKnolwedgeBase ()
	{
		return onto;
	}

	@Override
	protected void finalize () throws Throwable
	{
		this.poolSizeTunerTimerTask.stop ();
		super.finalize ();
	}
}
