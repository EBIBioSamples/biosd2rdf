package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.RandomUtils;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.resources.Resources;

/**
 * Exports a single {@link BioSample}. This is invoked by the {@link MSIExportTask} constructor, when a submission has 
 * many samples. We have this behaviour, because when this happens, the serial export of samples takes too much 
 * time. 
 *  
 * @author brandizi
 * <dl><dt>Date:</dt><dd>15 Dec 2015</dd></dl>
 *
 */
public class BioSampleExportTask extends BioSdExportTask
{
	private Long sampleId;
	private Map<String, Object> params;
	
	/**
	 * A priority between 0 and 10 is assigned to the task, in order to avoid starve the {@link MSIExportTask submission}
	 * tasks being trying to send in new samples.
	 * 
	 * @param rdfMapFactory
	 * @param sample
	 * @param params
	 */
	public BioSampleExportTask ( BioSdRfMapperFactory rdfMapFactory, BioSample sample, Map<String, Object> params ) 
	{
		super ( "XPORT:" + sample.getAcc (), rdfMapFactory );
		this.sampleId = sample.getId ();
		this.params = params;
		this.setPriority ( RandomUtils.nextInt ( 0, 11 ) );
	}


	@Override
	@SuppressWarnings ( "serial" )
	public void run ()
	{
		EntityManager em = null;
		BioSample sample = null;
		
		try
		{
			if ( sampleId != null )
			{
				// We got unsaved samples during tests, if they have an ID instead, we need to reload them, cause we're 
				// using another entity manager now
				
				em = Resources.getInstance ().getEntityManagerFactory ().createEntityManager ();

				sample = em.find ( 
					BioSample.class, 
					sampleId, 
					new HashMap<String, Object> () { { put ( "org.hibernate.readOnly", true ); } } 
				);
			}
			
			if ( sample == null ) {
				log.trace ( "Ignoring Sample ID #{}, null sample retrieved", sample  );
				return;
			}
			this.rdfMapFactory.map ( sample, params );
		} 
		catch ( Throwable ex ) 
		{
			// TODO: proper exit code
			log.error ( 
				"Error while exporting " + ( sampleId == null ? null : sample.getAcc () ) + ": " + ex.getMessage (),
				ex 
			);
			exitCode = 1;
		}
		finally {
			if ( em != null && em.isOpen () ) em.close ();
		}
	}

}
