/*
 * 
 */
package uk.ac.ebi.fg.java2rdf.utils;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Mar 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class OwlApiUtils
{
	public static void assertData ( OWLOntology model, String individualUri, String propertyUri, String propertyValue )
	{
		OWLOntologyManager owlMgr = model.getOWLOntologyManager ();
		OWLDataFactory owlFactory = owlMgr.getOWLDataFactory ();
		
		owlMgr.addAxiom ( model, owlFactory.getOWLDataPropertyAssertionAxiom ( 
			owlFactory.getOWLDataProperty ( IRI.create( propertyUri )), 
			owlFactory.getOWLNamedIndividual ( IRI.create ( individualUri )),  
			propertyValue 
		));
	}

	public static void assertLink ( OWLOntology model, String subjectUri, String propertyUri, String objectUri )
	{
		OWLOntologyManager owlMgr = model.getOWLOntologyManager ();
		OWLDataFactory owlFactory = owlMgr.getOWLDataFactory ();

		owlMgr.addAxiom ( model, owlFactory.getOWLObjectPropertyAssertionAxiom ( 
			owlFactory.getOWLObjectProperty ( IRI.create( propertyUri )), 
			owlFactory.getOWLNamedIndividual ( IRI.create ( subjectUri )),  
			owlFactory.getOWLNamedIndividual ( IRI.create ( objectUri ))  
		));
	}
	
	public static void assertIndividual ( OWLOntology model, String individualUri, String classUri ) 
	{
		OWLOntologyManager owlMgr = model.getOWLOntologyManager ();
		OWLDataFactory owlFactory = owlMgr.getOWLDataFactory ();
		
		owlMgr.addAxiom ( model, owlFactory.getOWLClassAssertionAxiom (
			owlFactory.getOWLClass ( IRI.create ( classUri )), 
			owlFactory.getOWLNamedIndividual( IRI.create ( individualUri ))
		));		
	}

}
