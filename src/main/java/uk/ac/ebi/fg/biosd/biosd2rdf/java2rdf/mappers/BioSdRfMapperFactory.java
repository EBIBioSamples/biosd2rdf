/*
 * 
 */
package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.registerNs;

import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapperFactory;

/**
 * The mapping entry point for the BioSD-to-RDF converter.
 * @See {@link BeanRdfMapperFactory}.
 *
 * <dl><dt>date</dt><dd>Apr 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdRfMapperFactory extends BeanRdfMapperFactory
{
	static {
		init ();
	}
	
	public static void init() 
	{
		registerNs ( "biosd", 	 				"http://rdf.ebi.ac.uk/resource/biosamples/" );
		registerNs ( "biosd-dataset",		"http://rdf.ebi.ac.uk/dataset/biosamples" );
		registerNs ( "ebi-term",  			"http://rdf.ebi.ac.uk/terms/" );
		registerNs ( "biosd-term",  		"http://rdf.ebi.ac.uk/terms/biosd/" );
		registerNs ( "obo", 		 				"http://purl.obolibrary.org/obo/" );
		registerNs ( "efo",			 				"http://www.ebi.ac.uk/efo/" );
	}
	
	{
		setMapper ( MSI.class, new MSIRdfMapper () );
		setMapper ( BioSample.class, new BioSampleRdfMapper () );
		setMapper ( BioSampleGroup.class, new BioSampleGroupRdfMapper () );
	}
	
	protected BioSdRfMapperFactory () {
		super ();
	}

	protected BioSdRfMapperFactory ( OWLOntology knowledgeBase ) {
		super ( knowledgeBase );
	}
	
}
