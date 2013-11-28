package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.utils.threading.BatchServiceTask;

/**
 * Exports a single SampleTab submission, using {@link BioSdRfMapperFactory} and the definitions based on the 
 * Java2RDF framework.
 *
 * <dl><dt>date</dt><dd>19 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdExportTask extends BatchServiceTask
{
	private MSI msi;
	private BioSdRfMapperFactory rdfMapFactory;

	private EntityManager em = null; 

	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private BioSdExportTask ( BioSdRfMapperFactory rdfMapFactory )
	{
		super ( "XPORT:" );
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
		this.name += msi.getAcc ();
	}

	public BioSdExportTask ( BioSdRfMapperFactory rdfMapFactory, Long msiId )
	{
		this ( rdfMapFactory );

		em = Resources.getInstance ().getEntityManagerFactory ().createEntityManager ();
		
		@SuppressWarnings ( "serial" )
		MSI msi = em.find ( MSI.class, msiId, new HashMap<String, Object> () { { put ( "org.hibernate.readOnly", true ); } } );

		if ( msi == null ) 
		{
			this.exitCode = 1;
			throw new IllegalArgumentException ( "Cannot find SampleTab submission #" + msiId );
		}
		this.msi = msi;
		this.name += msi.getAcc ();
	}

	/**
	 * Submits a SampleTab submission having the parameter accession. Used in the command line form: 'export acc...'
	 */
	public BioSdExportTask ( BioSdRfMapperFactory rdfMapFactory, String msiAcc )
	{
		this ( rdfMapFactory );

		em = Resources.getInstance ().getEntityManagerFactory ().createEntityManager ();
		
		Query q = em.createQuery ( "FROM MSI WHERE acc = :acc" );
		q.setParameter ( "acc", msiAcc );
		q.setHint("org.hibernate.readOnly", true);
		
		List<MSI> msis = (List<MSI>) q.getResultList ();

		if ( msis == null || msis.isEmpty () ) 
		{
			this.exitCode = 1;
			throw new IllegalArgumentException ( "Cannot find SampleTab submission #" + msiAcc );
		}
		this.msi = msis.iterator ().next ();
		this.name += this.msi.getAcc ();
	}

	@Override
	public void run ()
	{
		try
		{
			rdfMapFactory.map ( msi );
			/* DEBUG log.info ( "Here I should export {}, having a nap instead", msi.getAcc () );
			Thread.sleep ( RandomUtils.nextLong () % 10000 ); */
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
}
