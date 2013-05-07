/*
 * 
 */
package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map.Entry;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import uk.ac.ebi.fg.biosd.model.utils.test.TestModel;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.*;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Apr 24, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BioSdMappersTest
{
	@Test
	public void testMockupModel () throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException
	{
		OWLOntologyManager owlMgr = OWLManager.createOWLOntologyManager ();
		OWLOntology onto = owlMgr.createOntology ( IRI.create ( ns ( "biosd", "ontology" ) ) );

		TestModel biosdModel = new TestModel ();
		biosdModel.smp1.addPropertyValue ( new ExperimentalPropertyValue<> ( "Sample 1", new ExperimentalPropertyType ("name") ) );
		new BioSdRfMapperFactory ( onto ).map ( biosdModel.msi );
		
		PrefixOWLOntologyFormat fmt = new TurtleOntologyFormat ();
		fmt = new RDFXMLOntologyFormat ();
		for ( Entry<String, String> nse: getNamespaces ().entrySet () )
			fmt.setPrefix ( nse.getKey (), nse.getValue () );
		owlMgr.saveOntology ( onto, fmt, new FileOutputStream ( new File ( "target/biosd.owl" ) ));
	}
}
