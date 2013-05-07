/*
 * 
 */
package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapperFactory;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Apr 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdRfMapperFactory extends BeanRdfMapperFactory
{
	{
		setMapper ( MSI.class, new MSIRdfMapper () );
		setMapper ( BioSample.class, new BioSampleRdfMapper () );
	}
	
	protected BioSdRfMapperFactory () {
		super ();
	}

	protected BioSdRfMapperFactory ( OWLOntology knowledgeBase ) {
		super ( knowledgeBase );
	}

	
}
