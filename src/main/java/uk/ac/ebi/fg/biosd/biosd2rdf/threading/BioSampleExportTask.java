package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import java.util.Map;

import javax.persistence.EntityManager;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.IdentifiableDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;

/**
 * Exports a single {@link BioSample}. This is invoked by the {@link MSIExportTask} constructor, when a submission has 
 * too many samples. We have this behaviour, because when this happens, the serial export of samples takes too much 
 * time. 
 *  
 * @author brandizi
 * <dl><dt>Date:</dt><dd>15 Dec 2015</dd></dl>
 *
 */
public class BioSampleExportTask extends BioSdExportTask
{
	private BioSample sample;
	private Map<String, Object> params;
	
	public BioSampleExportTask ( BioSdRfMapperFactory rdfMapFactory, BioSample sample, Map<String, Object> params ) 
	{
		super ( "XPORT: " + sample.getAcc (), rdfMapFactory );
		this.sample = sample;
		this.params = params;
	}


	@Override
	public void run ()
	{
		EntityManager em = null;

		try
		{
			Long sampleId = this.sample.getId ();
			if ( sampleId != null )
			{
				// We got unsaved samples during tests, if they have an ID instead, we need to reload them, cause we're 
				// using another entity manager now
				
				em = Resources.getInstance ().getEntityManagerFactory ().createEntityManager ();
				IdentifiableDAO<BioSample> sampleDao = new IdentifiableDAO<BioSample> ( BioSample.class, em );
			
				this.sample = sampleDao.find ( sampleId );
			}
			
			if ( this.sample == null ) {
				log.trace ( "Ignoring Sample ID #{}", this.sample  );
			}
			this.rdfMapFactory.map ( this.sample, params );
		} 
		catch ( Throwable ex ) 
		{
			// TODO: proper exit code
			log.error ( "Error while exporting " + this.sample.getAcc () + ": " + ex.getMessage (), ex );
			exitCode = 1;
		}
		finally {
			if ( em != null && em.isOpen () ) em.close ();
		}
	}

}
