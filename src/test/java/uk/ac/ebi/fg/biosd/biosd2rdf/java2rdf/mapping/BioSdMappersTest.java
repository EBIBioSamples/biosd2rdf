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
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import uk.ac.ebi.fg.biosd.biosd2rdf.utils.SparqlBasedTester;
import uk.ac.ebi.fg.biosd.model.utils.test.TestModel;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRecordRef;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicType;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.Unit;
import uk.ac.ebi.fg.core_model.expgraph.properties.UnitDimension;
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
		
		biosdModel.cv1.setTermText ( "Mus Musculus" ); // ZOOMA likes this more than 'mus-mus'
		
		biosdModel.msi.setTitle ( "A test SampleTab Submission" );
		biosdModel.msi.setDescription ( "Hey! This is just a test! And this is a bad XML char: \u0015" );
		
		BioCharacteristicValue mycv = new BioCharacteristicValue ( "My mouse", new BioCharacteristicType ( "My specie" ) );
		// Mus mus subspecies
		mycv.addOntologyTerm ( new OntologyEntry ( "EFO_0003013", new ReferenceSource ( "EFO", null ) ) );
		biosdModel.smp1.addPropertyValue ( mycv );

		BioCharacteristicValue mycv1 = new BioCharacteristicValue ( "Liver", new BioCharacteristicType ( "Organ" ) );
		// This should be taken as-is, cause it's a URI without source
		mycv1.addOntologyTerm ( new OntologyEntry ( "http://sig.uw.edu/fma#Liver", null ) );
		biosdModel.smp1.addPropertyValue ( mycv1 );

		biosdModel.smp1.addPropertyValue ( new BioCharacteristicValue ( "Sample 1", new BioCharacteristicType ("name") ) );

		
		
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
		
		DatabaseRecordRef db = new DatabaseRecordRef ( "ArrayExpress", "E-GEOD-12040", null );
		db.setUrl ( "http://www.ebi.ac.uk/arrayexpress/experiments/E-GEOD-12040" );
		biosdModel.msi.addDatabaseRecordRef ( db );

		DatabaseRecordRef db1 = new DatabaseRecordRef ( "ArrayExpress", "E-GEOD-12040-smp1", null );
		db1.setUrl ( "http://www.ebi.ac.uk/arrayexpress/experiments/E-GEOD-12040/smp1" );
		biosdModel.smp1.addDatabaseRecordRef ( db1 );

		
		// This should generate specific statements about the value and the unit.
		biosdModel.cv5.setTermText ( "2.5" );
		biosdModel.cv5.setUnit ( new Unit ( "percent", null ) );

		// The URI for this is given explictly and we expect the exported use this.
		Unit uoUnit = new Unit ( "weeks", new UnitDimension ( "Time" ) );
		uoUnit.addOntologyTerm ( new OntologyEntry ( "UO_0000034", new ReferenceSource ( "UO", null ) ) );
		BioCharacteristicValue age = new BioCharacteristicValue ( "2-5", new BioCharacteristicType ( "Age" ) );
		age.setUnit ( uoUnit );
		biosdModel.smp1.addPropertyValue ( age );

		// This should solve to grams and not to the prefix giga-
		Unit gUnit = new Unit ( "g", null );
		BioCharacteristicValue weight = new BioCharacteristicValue ( "250", new BioCharacteristicType ( "Weight" ) );
		weight.setUnit ( gUnit );
		biosdModel.smp1.addPropertyValue ( weight );
		
		
		// Zooma should solve this into EFO_0004950 and the value should be an xsd:dateTime
		biosdModel.smp1.addPropertyValue ( new BioCharacteristicValue ( "31/12/2012", new BioCharacteristicType ( "Date of Birth" ) ) );
		
		
		// We currently set this at submission level only
		biosdModel.msi.setPublicFlag ( true );
		
		new BioSdRfMapperFactory ( onto ).map ( biosdModel.msi );
		
		PrefixOWLOntologyFormat fmt = new TurtleOntologyFormat ();
		for ( Entry<String, String> nse: getNamespaces ().entrySet () )
			fmt.setPrefix ( nse.getKey (), nse.getValue () );
		owlMgr.saveOntology ( onto, fmt, new FileOutputStream ( new File ( "target/test_model.ttl" ) ));

	
	  // Now tests stuff
		SparqlBasedTester tester = new SparqlBasedTester ( "target/test_model.ttl", 
			"PREFIX biosd-terms: <http://rdf.ebi.ac.uk/terms/biosd/>\n" +
			"PREFIX dc-terms: <http://purl.org/dc/terms/>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX efo: <http://www.ebi.ac.uk/efo/>\n" +
			"PREFIX obo: <http://purl.obolibrary.org/obo/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
			"PREFIX bibo: <http://purl.org/ontology/bibo/>\n" +
			"PREFIX sio: <http://semanticscience.org/resource/>\n" +
			"PREFIX prism: <http://prismstandard.org/namespaces/basic/2.0/>\n" +
			"PREFIX fabio: <http://purl.org/spar/fabio/>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX pav: <http://purl.org/pav/2.0/>\n" +
		  "\n"
		);
		
		tester.testRDFOutput ( "Test submission not found!", 
			"ASK {\n"
			+ "  <http://rdf.ebi.ac.uk/resource/biosamples/msi/" + biosdModel.msi.getAcc () + "> a biosd-terms:BiosamplesSubmission;\n"
			+ "    dc-terms:title '" + biosdModel.msi.getTitle () + "';\n"
			+ "    rdfs:label '" + biosdModel.msi.getTitle () + "';\n"
			+ "    rdfs:comment '" + biosdModel.msi.getDescription () + "';\n"
			+ "    dc-terms:description '" + biosdModel.msi.getDescription () + "';\n"
			+ "    dc-terms:identifier '" + biosdModel.msi.getAcc () + "'.\n"
			+ "}\n"		
		);

		
		tester.testRDFOutput ( "Submission contents not found!", 
			"ASK {\n"
			+ "  <http://rdf.ebi.ac.uk/resource/biosamples/msi/" + biosdModel.msi.getAcc () + "> a biosd-terms:BiosamplesSubmission;\n"
			+ "    obo:IAO_0000219"
			+ "        <http://rdf.ebi.ac.uk/resource/biosamples/sample-group/sg1>,\n"
      + "        <http://rdf.ebi.ac.uk/resource/biosamples/sample-group/sg2>, \n"
      + "        <http://rdf.ebi.ac.uk/resource/biosamples/sample/smp1>,\n"
      + "        <http://rdf.ebi.ac.uk/resource/biosamples/sample/smp2>,\n"
      + "        <http://rdf.ebi.ac.uk/resource/biosamples/sample/smp3>,\n"
      + "        <http://rdf.ebi.ac.uk/resource/biosamples/sample/smp4>,\n"
      + "        <http://rdf.ebi.ac.uk/resource/biosamples/sample/smp5>,\n"
      + "        <http://rdf.ebi.ac.uk/resource/biosamples/sample/smp6>.\n"
			+ "}\n"		
		);
		
		tester.testRDFOutput ( "smp1 details not found!", 
			"ASK {\n"
			+ "  <http://rdf.ebi.ac.uk/resource/biosamples/sample/smp1> a biosd-terms:Sample;\n"
			+ "    dc-terms:title 'Sample 1'\n"
			+ "}\n"		
		);
		
		tester.testRDFOutput ( "explicit ontology annotation not found!", 
			"ASK {\n"
			+ "  ?smp a biosd-terms:Sample;\n"
			+ "  biosd-terms:has-bio-characteristic [ "
			+ "    biosd-terms:has-bio-characteristic-type ?ptype, ?ptype1;\n"
			+ "    rdfs:label 'My mouse';\n"
			+ "  ].\n"
			+ "\n"
			+ "  ?ptype a efo:EFO_0000001;\n"
			+ "    dc-terms:title 'My specie'.\n"
			+ "\n"
			+ "  ?ptype1 a efo:EFO_0003013"
			+ "}\n"		
		);

		tester.testRDFOutput ( "Direct-URI ontology annotation not found!", 
				"ASK {\n"
				+ "  ?smp a biosd-terms:Sample;\n"
				+ "  biosd-terms:has-bio-characteristic [ "
				+ "    biosd-terms:has-bio-characteristic-type ?ptype, ?ptype1;\n"
				+ "    rdfs:label 'Liver';\n"
				+ "  ].\n"
				+ "\n"
				+ "  ?ptype a efo:EFO_0000001;\n"
				+ "    dc-terms:title 'Organ'.\n"
				+ "\n"
				+ "  ?ptype1 a <http://sig.uw.edu/fma#Liver>"
				+ "}\n"		
			);

		
		
		tester.testRDFOutput ( "ZOOMA-based annotation not found!", 
				"ASK {\n"
				+ "  ?smp a biosd-terms:Sample;\n"
				+ "  biosd-terms:has-bio-characteristic [ "
				+ "    biosd-terms:has-bio-characteristic-type ?ptype, ?ptype1;\n"
				+ "    rdfs:label 'Mus Musculus';\n"
				+ "  ].\n"
				+ "\n"
				+ "  ?ptype a efo:EFO_0000001;\n"
				+ "    dc-terms:title 'Organism'.\n"
				+ "\n"
				+ "  ?ptype1 a obo:NCBITaxon_10090"
				+ "}\n"		
			);
		
		tester.testRDFOutput ( "Publication statements not found!", 
			"ASK { <http://rdf.ebi.ac.uk/resource/biosamples/publication/123> rdf:type obo:IAO_0000311;\n"+
			" rdfs:label 'A test publication 1';\n"+
			" fabio:hasPubMedId '123';\n"+
			" bibo:pmid '123';\n"+
			" fabio:hasPublicationYear '2013'^^xsd:gYear ;\n"+
			" dc-terms:title 'A test publication 1';\n"+
			" biosd-terms:has-authors-list 'Test 1, A, Test 2, B, Test 3, C';\n"+
			" prism:doi 'doi://123';\n"+
			" bibo:doi 'doi://123';\n"+
			" obo:IAO_0000136 <http://rdf.ebi.ac.uk/resource/biosamples/msi/msi1>;\n"+
			" sio:SIO_000332 <http://rdf.ebi.ac.uk/resource/biosamples/msi/msi1> }" 
		);
		
		tester.testRDFOutput ( "Web Record for SG1 not found!",
		  "ASK {\n"
		  + "  <http://rdf.ebi.ac.uk/resource/biosamples/sample-group/sg1> a biosd-terms:SampleGroup;\n"
		  + "    rdfs:label 'Sample Group sg1';\n"
		  + "    pav:derivedFrom [\n"
		  + "      a biosd-terms:RepositoryWebRecord;\n"
		  + "      dc-terms:source 'EBI Biosamples Database';\n"
		  + "      foaf:page <http://www.ebi.ac.uk/biosamples/group/sg1>\n"
		  + "    ]\n"
		  + "}"		
		);
		
		tester.testRDFOutput ( "Number annotation wrong!", 
			"ASK {\n"
			+ "  ?smp a biosd-terms:Sample;\n"
			+ "  biosd-terms:has-bio-characteristic [ "
			+ "    rdfs:label '250 g';\n"
			+ "    sio:SIO_000300 '250'^^xsd:double;\n"
			+ "    sio:SIO_000221 ?unit"
			+ "  ].\n"
			+ "\n"
			+ "  ?unit a obo:UO_0000021, sio:SIO_000074; dc-terms:title 'g'."
			+ "}\n"		
		);

		tester.testRDFOutput ( "Range annotation wrong!", 
			"ASK {\n"
			+ "  ?smp a biosd-terms:Sample;\n"
			+ "  biosd-terms:has-bio-characteristic [ "
			+ "    a sio:SIO_000944;\n"
			+ "    dc-terms:title '2-5 weeks';\n"
			+ "    biosd-terms:has-low-value '2.0'^^xsd:double;\n"
			+ "    biosd-terms:has-high-value '5.0'^^xsd:double;\n"
			+ "    sio:SIO_000221 ?unit"
			+ "  ].\n"
			+ "\n"
			+ "  ?unit a obo:UO_0000034, sio:SIO_000074; rdfs:label 'weeks'."
			+ "}\n"		
		);
		
		tester.testRDFOutput ( "Date annotation wrong!", 
			"ASK {\n"
			+ "  ?smp a biosd-terms:Sample;\n"
			+ "  biosd-terms:has-bio-characteristic [ "
			+ "    dc-terms:title '31/12/2012';\n"
			+ "    sio:SIO_000300 ?date\n"
			+ "  ].\n"
			+ "  FILTER ( datatype( ?date ) = xsd:date )."
			+ "  FILTER ( REGEX ( STR ( ?date ), '2012-12-31' ) )." // Cannot test this directly, cause it's time zone-dependent
			+ "}\n"		
		);		
		
	}
}
