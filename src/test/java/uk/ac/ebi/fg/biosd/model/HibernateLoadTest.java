package uk.ac.ebi.fg.biosd.model;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.utils.test.junit.TestEntityMgrProvider;

import static java.lang.System.out;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>2 Aug 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class HibernateLoadTest
{
	@Rule
	public TestEntityMgrProvider emProvider = new TestEntityMgrProvider ( Resources.getInstance ().getEntityManagerFactory () );
	
	@Test
	public void testSampleLoad ()
	{
		int ct = 0;
		EntityManager em = emProvider.getEntityManager ();
		for ( MSI msi: (List<MSI>) em.createQuery ( "FROM MSI" ).getResultList () )
		{
			out.println ( msi.getAcc () );
			for ( BioSample smp: msi.getSamples () )
			{
				out.println ( "  " + smp.getAcc () );
				for ( ExperimentalPropertyValue<ExperimentalPropertyType> pv: smp.getPropertyValues () )
				  out.println ( "    " + pv );
			}
			out.println ();
			if ( ++ct > 10 ) break;
		}
	}
}
