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

import uk.ac.ebi.fg.biosd.biosd2rdf.utils.AnnotatorHelper;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentType;
import uk.ac.ebi.fg.biosd.model.expgraph.properties.SampleCommentValue;
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
import uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils;
import uk.ac.ebi.fg.java2rdf.utils.test.SparqlBasedTester;

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
		
		biosdModel.cv1.setTermText ( "Mus Musculus" );
		
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

		// Special relations translated as provenance
		biosdModel.smp1.addPropertyValue ( new BioCharacteristicValue ( "smp0", new BioCharacteristicType ( "Derived From" ) ) );
		biosdModel.smp1.addPropertyValue ( new BioCharacteristicValue ( "smp999", new BioCharacteristicType ( "Derived To" ) ) );

		// Testing "Same As"
		BioCharacteristicValue sameAsRel = new BioCharacteristicValue ( "smp1bis", new BioCharacteristicType ( "Same As" ) );
		biosdModel.smp1.addPropertyValue ( sameAsRel );
		
		// Testing "Child Of/Parent Of"
		BioCharacteristicValue childOfRel = new BioCharacteristicValue ( "smp1Parent", new BioCharacteristicType ( "Child Of" ) );
		biosdModel.smp1.addPropertyValue ( childOfRel );
		BioCharacteristicValue parentOfRel = new BioCharacteristicValue ( "smp1Child", new BioCharacteristicType ( "Parent Of" ) );
		biosdModel.smp1.addPropertyValue ( parentOfRel );

		// References
		biosdModel.msi.addSampleRef ( "referredSmp1" );
		biosdModel.msi.addSampleRef ( "referredSmp2" );
		biosdModel.msi.addSampleGroupRef ( "referredSG1" );
		biosdModel.msi.addSampleGroupRef ( "referredSG2" );

		
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
		
		
		// ZOOMA should solve this into EFO_0004950 and the value should be an xsd:dateTime
		biosdModel.smp1.addPropertyValue ( new BioCharacteristicValue ( "31/12/2012", new BioCharacteristicType ( "Date of Birth" ) ) );
		
		// To test 'has-sample-attribute'
		// This should get the ontology term EFO_0000565
		biosdModel.smp1.addPropertyValue ( new SampleCommentValue ( "Leukemia", new SampleCommentType ( "Disease State" ) ) );
		
		// We currently set this at submission level only
		biosdModel.msi.setPublicFlag ( true );
		
		// Generate the annotations to be used for RDF
		AnnotatorHelper annHelper = new AnnotatorHelper ();
		annHelper.begin ();
		annHelper.annotate ( biosdModel.msi );
		annHelper.commit ();
		
		new BioSdRfMapperFactory ( onto ).map ( biosdModel.msi );
		
		PrefixOWLOntologyFormat fmt = new TurtleOntologyFormat ();
		for ( Entry<String, String> nse: getNamespaces ().entrySet () )
			fmt.setPrefix ( nse.getKey (), nse.getValue () );
		owlMgr.saveOntology ( onto, fmt, new FileOutputStream ( new File ( "target/test_model.ttl" ) ));

	
	  // Now tests stuff
		SparqlBasedTester tester = new SparqlBasedTester ( 
			"target/test_model.ttl", 
			NamespaceUtils.asSPARQLProlog () 
			+ "PREFIX smp: <http://rdf.ebi.ac.uk/resource/biosamples/sample/>\n"
			+ "PREFIX sg: <http://rdf.ebi.ac.uk/resource/biosamples/sample-group/>\n"
			+ "PREFIX msi: <http://rdf.ebi.ac.uk/resource/biosamples/msi/>\n"
			+ "PREFIX msipub: <http://rdf.ebi.ac.uk/resource/biosamples/publication/>\n"
			+ "\n\n"
		);
		
		tester.testRDFOutput ( "Test submission not found!", 
			"ASK {\n"
			+ "  msi:" + biosdModel.msi.getAcc () + " a biosd-terms:BiosamplesSubmission;\n"
			+ "    dc-terms:title '" + biosdModel.msi.getTitle () + "';\n"
			+ "    rdfs:label '" + biosdModel.msi.getTitle () + "';\n"
			+ "    rdfs:comment '" + biosdModel.msi.getDescription () + "';\n"
			+ "    dc-terms:description '" + biosdModel.msi.getDescription () + "';\n"
			+ "    dc-terms:identifier '" + biosdModel.msi.getAcc () + "'.\n"
			+ "}\n"		
		);

		
		tester.testRDFOutput ( "Submission contents not found!", 
			"ASK {\n"
			+ "  msi:" + biosdModel.msi.getAcc () + " a biosd-terms:BiosamplesSubmission;\n"
			+ "    obo:IAO_0000219"
			+ "        sg:sg1, sg:sg1,\n"
      + "        smp:smp1, smp:smp1, smp:smp3, smp:smp4, smp:smp5, smp:smp6.\n"
			+ "}\n"		
		);
		
		
		
		tester.testRDFOutput ( "smp1 details not found!", 
			"ASK {\n"
			+ "  smp:smp1 a biosd-terms:Sample;\n"
			+ "    dc-terms:title 'Sample 1'\n"
			+ "}\n"		
		);

		tester.testRDFOutput ( "smp1 derivedFrom smp0 not found! (via special property)", 
			"ASK {\n"
			+ "  smp:smp1 prov:wasDerivedFrom smp:smp0.\n"
			+ "  smp:smp0 prov:hadDerivation smp:smp1.\n"
			+ "  smp:smp1 sio:SIO_000244 smp:smp0.\n"
			+ "  smp:smp0 sio:SIO_000245 smp:smp1.\n"
			+ "}\n"		
		);

		tester.testRDFOutput ( "smp1 derivedInto smp999 not found! (via special property)", 
			"ASK {\n"
			+ "  smp:smp999 prov:wasDerivedFrom smp:smp1.\n"
			+ "  smp:smp1 prov:hadDerivation smp:smp999.\n"
			+ "  smp:smp999 sio:SIO_000244 smp:smp1.\n"
			+ "  smp:smp1 sio:SIO_000245 smp:smp999.\n"
			+ "}\n"		
		);
		
		tester.testRDFOutput ( "smp3 derivedFrom smp1 not found!", 
			"ASK {\n"
			+ "  smp:smp3 prov:wasDerivedFrom smp:smp1.\n"
			+ "  smp:smp1 prov:hadDerivation smp:smp3.\n"
			+ "  smp:smp3 sio:SIO_000244 smp:smp1.\n"
			+ "  smp:smp1 sio:SIO_000245 smp:smp3.\n"
			+ "}\n"		
		);
		
		tester.testRDFOutput ( "smp1 sameAs smp1bis not found! ", 
			"ASK {\n"
			+ "  smp:smp1 owl:sameAs smp:smp1bis.\n"
			+ "  smp:smp1bis owl:sameAs smp:smp1.\n"
			+ "}\n"		
		);

		tester.testRDFOutput ( "smp1 biosd-terms:sample-child-of smp1Parent not found! ", 
			"ASK {\n"
			+ "  smp:smp1 biosd-terms:sample-child-of smp:smp1Parent.\n"
			+ "  smp:smp1Parent biosd-terms:sample-parent-of smp:smp1.\n"
			+ "}\n"		
		);

		tester.testRDFOutput ( "smp1 biosd-terms:sample-parent-of smp1Child not found! ", 
			"ASK {\n"
			+ "  smp:smp1 biosd-terms:sample-parent-of smp:smp1Child.\n"
			+ "  smp:smp1Child biosd-terms:sample-child-of smp:smp1.\n"
			+ "}\n"		
		);

		
		tester.testRDFOutput ( "References from msi1 to samples not found!", 
			"ASK {\n"
			+ "  msi:msi1 rdfs:seeAlso smp:referredSmp1, smp:referredSmp1.\n"
			+ "}\n"		
		);

		tester.testRDFOutput ( "References from msi1 to sample group not found!", 
			"ASK {\n"
			+ "  msi:msi1 rdfs:seeAlso sg:referredSG1, sg:referredSG2.\n"
			+ "}\n"		
		);		
		
		tester.testRDFOutput ( "Explicit ontology annotation not found!", 
			"ASK {\n"
			+ "  ?smp a biosd-terms:Sample;\n"
			+ "  biosd-terms:has-bio-characteristic [ "
			+ "    a efo:EFO_0000001, efo:EFO_0003013;\n"
			+ "    dc:type 'My specie';\n"
			+ "    atlas:propertyType 'My specie';\n"
			+ "    rdfs:label 'My mouse';\n"
			+ "    atlas:propertyValue 'My mouse'\n"
			+ "  ]\n"
			+ "}\n"		
		);
		
		if ( ExpPropValueRdfMapper.OLD_MODEL_SUPPORT_FLAG )
			tester.testRDFOutput ( "Explicit ontology annotation not found (old model)!", 
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
			+ "    a efo:EFO_0000001, <http://sig.uw.edu/fma#Liver>;\n"
			+ "    dc:type 'Organ';\n"
			+ "    atlas:propertyType 'Organ';\n"
			+ "    rdfs:label 'Liver';\n"
			+ "    atlas:propertyValue 'Liver'\n"
			+ "  ]\n"
			+ "}\n"		
		);
		
		if ( ExpPropValueRdfMapper.OLD_MODEL_SUPPORT_FLAG )
			tester.testRDFOutput ( "Direct-URI ontology annotation not found (old model)!", 
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
				
		
		tester.testRDFOutput ( "Text mining-based annotation not found!", 
			"ASK {\n"
			+ "  ?smp a biosd-terms:Sample;\n"
			+ "  biosd-terms:has-bio-characteristic [ "
			+ "    a efo:EFO_0000001, obo:NCBITaxon_10090;\n"
			+ "    dc:type 'Organism';\n"
			+ "    atlas:propertyType 'Organism';\n"
			+ "    rdfs:label 'Mus Musculus';\n"
			+ "    atlas:propertyValue 'Mus Musculus'\n"
			+ "  ]\n"
			+ "}\n"		
		);

		tester.testRDFOutput ( "Text mining-based annotation provenance not found!", 
			"ASK {\n"
			+ "  ?smp a biosd-terms:Sample;\n"
			+ "  biosd-terms:has-bio-characteristic ?pv.\n"
			+ "  ?pv a obo:NCBITaxon_10090.\n"
			+ "  ?ann a biosd-terms:SampleAttributeOntologyAnnotation;\n"
			+ "       oac:hasTarget ?pv; oac:hasBody obo:NCBITaxon_10090;\n"
			+ "       dc:creator ?prov; biosd-terms:has-percent-score ?score"
			+ "}\n"		
		);

		
		
		
		if ( ExpPropValueRdfMapper.OLD_MODEL_SUPPORT_FLAG )
			tester.testRDFOutput ( "Text mining-based annotation not found (old model)!", 
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
		

		// Check that has-sample-attribute was used for Comment[]
		//
		tester.testRDFOutput ( "Text mining-based annotation not found!", 
			"ASK {\n"
			+ "  ?smp a biosd-terms:Sample;\n"
			+ "  biosd-terms:has-sample-attribute [ "
			+ "    a efo:EFO_0000001, efo:EFO_0000565;\n"
			+ "    dc:type 'Disease State';\n"
			+ "    atlas:propertyType 'Disease State';\n"
			+ "    rdfs:label 'Leukemia';\n"
			+ "    atlas:propertyValue 'Leukemia'\n"
			+ "  ]\n"
			+ "}\n"		
		);
		
		if ( ExpPropValueRdfMapper.OLD_MODEL_SUPPORT_FLAG )
			tester.testRDFOutput ( "Text mining-based annotation not found (old model)!", 
				"ASK {\n"
				+ "  ?smp a biosd-terms:Sample;\n"
				+ "  sio:SIO_000332 [ " // is about
				+ "    a efo:EFO_0000001;"
				+ "    rdfs:label 'Leukemia';\n"
				+ " 	 biosd-terms:has-bio-characteristic-type\n"
				+ "		   [ a efo:EFO_0000565 ],\n"
				+ "	     [ a efo:EFO_0000001; rdfs:label 'Disease State' ]\n"
				+ "  ]\n"
				+ "}\n"		
		);

		
	
		
		
		tester.testRDFOutput ( "Publication statements not found!", 
			"ASK { msipub:123 rdf:type obo:IAO_0000311;\n"+
			" rdfs:label 'A test publication 1';\n"+
			" fabio:hasPubMedId '123';\n"+
			" bibo:pmid '123';\n"+
			" fabio:hasPublicationYear '2013'^^xsd:gYear ;\n"+
			" dc-terms:title 'A test publication 1';\n"+
			" biosd-terms:has-authors-list 'Test 1, A, Test 2, B, Test 3, C';\n"+
			" prism:doi 'doi://123';\n"+
			" bibo:doi 'doi://123';\n"+
			" obo:IAO_0000136 msi:msi1;\n"+
			" sio:SIO_000332 msi:msi1 }" 
		);
		
		tester.testRDFOutput ( "Web Record for SG1 not found!",
		  "ASK {\n"
		  + "  sg:sg1 a biosd-terms:SampleGroup;\n"
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
