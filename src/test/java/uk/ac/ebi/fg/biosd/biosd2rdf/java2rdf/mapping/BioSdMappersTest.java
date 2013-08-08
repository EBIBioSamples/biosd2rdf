package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.getNamespaces;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map.Entry;

import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.model.utils.test.TestModel;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;

/**
 * Basic test of the BioSD-to-RDF mapping machinery.
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
		BioSdRfMapperFactory.init (); // cause the definition of BioSD-specific namespaces
		
		OWLOntologyManager owlMgr = OWLManager.createOWLOntologyManager ();
		OWLOntology onto = owlMgr.createOntology ( IRI.create ( ns ( "biosd-dataset" ) ) );

		TestModel biosdModel = new TestModel ();
		
		biosdModel.msi.setTitle ( "A test SampleTab Submission" );
		biosdModel.msi.setDescription ( "Hey! This is just a test!" );
		
		biosdModel.smp1.addPropertyValue ( new ExperimentalPropertyValue<> ( "Sample 1", new ExperimentalPropertyType ("name") ) );
		biosdModel.ch1.addOntologyTerm ( new OntologyEntry ( "EFO_0000634", new ReferenceSource ( "EFO", null ) ) );
		
		Publication pub1 = new Publication ( "doi://123", "123" );
		pub1.setTitle ( "A test publication 1" );
		pub1.setAuthorList ( "Test 1, A, Test 2, B, Test 3, C" );
		pub1.setYear ( "2013" );
		biosdModel.msi.addPublication ( pub1 );

		Publication pub2 = new Publication ( null, null );
		pub2.setTitle ( "A test publication 2" );
		pub2.setAuthorList ( "Test A, AA, Test B, BB, Test C, CC" );
		biosdModel.msi.addPublication ( pub2 );
		
		Contact cnt = new Contact ();
		cnt.setFirstName ( "Mr" );
		cnt.setLastName ( "Test" );
		cnt.setEmail ( "mr.test@somewhere.net" );
		cnt.setAffiliation ( "The Test Institute" );
		cnt.setAddress ( "Some Street in some Town somewhere in the Universe" );
		cnt.setUrl ( "http://somewhere.net" );
		biosdModel.msi.addContact ( cnt );
		
		Organization org = new Organization ();
		org.setName ( "The Test Organisation" );
		org.setEmail ( "info@somewher.net" );
		org.setAddress ( "Some Street in some Town somewhere in the Universe" );
		org.setPhone ( "123-456-789" );
		org.setDescription ( "A test Organisation" );
		biosdModel.msi.addOrganization ( org );
		
		new BioSdRfMapperFactory ( onto ).map ( biosdModel.msi );
		
		PrefixOWLOntologyFormat fmt = new TurtleOntologyFormat ();
		fmt = new RDFXMLOntologyFormat ();
		for ( Entry<String, String> nse: getNamespaces ().entrySet () )
			fmt.setPrefix ( nse.getKey (), nse.getValue () );
		owlMgr.saveOntology ( onto, fmt, new FileOutputStream ( new File ( "target/biosd.owl" ) ));
	}
}
