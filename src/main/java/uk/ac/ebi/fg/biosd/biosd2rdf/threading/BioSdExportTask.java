package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.IdentifiableDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>19 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdExportTask implements Runnable
{
	private MSI msi;
	private BioSdRfMapperFactory rdfMapFactory;
	private int exitCode = 0;

	private EntityManager em = null; 

	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private BioSdExportTask ( BioSdRfMapperFactory rdfMapFactory )
	{
		this.rdfMapFactory = rdfMapFactory;
	}
	
	public BioSdExportTask ( BioSdRfMapperFactory rdfMapFactory, MSI msi )
	{
		this ( rdfMapFactory );
		if ( msi == null ) 
		{
			this.exitCode = 1;
			throw new IllegalArgumentException ( "Internal error: cannot run an exporter over a null SampleTab submission" );
		}
		this.msi = msi;
	}

	public BioSdExportTask ( BioSdRfMapperFactory rdfMapFactory, Long msiId )
	{
		this ( rdfMapFactory );

		em = Resources.getInstance ().getEntityManagerFactory ().createEntityManager ();
		IdentifiableDAO<MSI> msiDao = new IdentifiableDAO<MSI> ( MSI.class, em );
		MSI msi = msiDao.find ( msiId );
		if ( msi == null ) 
		{
			this.exitCode = 1;
			throw new IllegalArgumentException ( "Cannot find SampleTab submission #" + msiId );
		}
		this.msi = msi;
	}
	
	@Override
	public void run ()
	{
		try
		{
			rdfMapFactory.map ( msi );
		} 
		catch ( Exception ex ) 
		{
			// TODO: proper exit code
			log.error ( "Error while exporting " + msi.getAcc () + ": " + ex.getMessage (), ex );
			exitCode = 1;
		}
		finally {
			if ( em != null && em.isOpen () ) em.close ();
		}
	}

	public int getExitCode ()
	{
		return exitCode;
	}

	public MSI getMSI ()
	{
		return msi;
	}
	
}
