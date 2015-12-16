package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.utils.threading.BatchServiceTask;

/**
 * A generic interface to represent a {@link BatchServiceTask} that is passed to
 * the {@link BioSdExportService}.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>15 Dec 2015</dd></dl>
 *
 */
public abstract class BioSdExportTask extends BatchServiceTask
{
	protected BioSdRfMapperFactory rdfMapFactory;

	protected BioSdExportTask ( String name, BioSdRfMapperFactory rdfMapFactory )
	{
		super ( name );
		this.rdfMapFactory = rdfMapFactory;
	}
	
}
