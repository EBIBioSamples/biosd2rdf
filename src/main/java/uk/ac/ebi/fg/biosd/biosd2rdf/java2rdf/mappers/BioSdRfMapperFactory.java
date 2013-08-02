/*
 * 
 */
package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.registerNs;

import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
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
		registerNs ( "ebi-terms",  			"http://rdf.ebi.ac.uk/terms/" );
		registerNs ( "biosd-term",  		"http://rdf.ebi.ac.uk/terms/biosd/" );
		registerNs ( "obo", 		 				"http://purl.obolibrary.org/obo/" );
		registerNs ( "efo",			 				"http://www.ebi.ac.uk/efo/" );
		registerNs ( "fabio",						"http://purl.org/spar/fabio/" ); // Biblio ontology
		registerNs ( "prism", 					"http://prismstandard.org/namespaces/basic/2.0/" ); // top-level for fabio
		registerNs ( "sch",							"http://schema.org/" );
		registerNs ( "foaf",						"http://xmlns.com/foaf/0.1/" );
	}
	
	{
		setMapper ( MSI.class, new MSIRdfMapper () );
		setMapper ( BioSample.class, new BioSampleRdfMapper () );
		setMapper ( BioSampleGroup.class, new BioSampleGroupRdfMapper () );
		setMapper ( Publication.class, new PublicationRdfMapper () );
		setMapper ( Contact.class, new ContactRdfMapper () );
		setMapper ( Organization.class, new OrganizationRdfMapper () );
	}
	
	public BioSdRfMapperFactory () {
		super ();
	}

	public BioSdRfMapperFactory ( OWLOntology knowledgeBase ) {
		super ( knowledgeBase );
	}

}
