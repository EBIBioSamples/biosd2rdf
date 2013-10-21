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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.utils.memory.MemoryUtils;
import uk.ac.ebi.utils.threading.BatchService;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>19 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdExportService extends BatchService<BioSdExportTask>
{
	private OWLOntology onto;
	private BioSdRfMapperFactory rdfMapFactory;
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private String outputPath = null;
	private int outputCounter = 0;
	private Runnable memFlushAction = new Runnable() 
	{
		@Override
		public void run () {
			BioSdExportService.this.flushKnowledgeBase ();
		}
	};

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
	
	public void submit ( Long msiId ) {
		this.submit ( new BioSdExportTask ( rdfMapFactory, msiId ) );
	}

	/** Used mainly for testing purposes. */
	public void submit ( MSI msi ) {
		this.submit ( new BioSdExportTask ( rdfMapFactory, msi ) );
	}

	@Override
	public void submit ( BioSdExportTask batchServiceTask )
	{
		// This will flush the triples to the disk when the memory is too full
		MemoryUtils.checkMemory ( this.memFlushAction, 15d / 100d );
		// DEBUG if ( completedTasks > 0 && completedTasks % 20 == 0 ) flushKnowledgeBase ();

		super.submit ( batchServiceTask );
	}

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
	
}
