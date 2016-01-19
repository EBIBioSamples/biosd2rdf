package uk.ac.ebi.fg.biosd.biosd2rdf.test;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.getNamespaces;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.bioportal.webservice.client.BioportalClient;
import uk.ac.ebi.bioportal.webservice.exceptions.OntologyServiceException;
import uk.ac.ebi.bioportal.webservice.model.OntologyClass;
import uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mapping.BioSdRfMapperFactory;
import uk.ac.ebi.fg.biosd.biosd2rdf.utils.BioSdOntologyTermResolver;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

/**
 * Generates some info that is needed to manually compare together annotations created with ZOOMA and Bioportal.
 * 
 * @author brandizi
 * <dl><dt>Date:</dt><dd>15 Jan 2016</dd></dl>
 *
 */
public class OntoDiscoverersComparisonTest
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	@Test @Ignore ( "Not a real test, just some quick hacking" )
	public void generateZoomaRDF () throws Exception
	{
		generateRDF ( "zooma", "top_100_attributes", "discoverer_test_top_100_zooma" );
	}

	@Test @Ignore ( "Not a real test, just some quick hacking" )
	public void generateBioportalRDF () throws Exception
	{
		generateRDF ( "bioportal", "top_100_attributes", "discoverer_test_top_100_bioportal" );
	}
	
	@Test @Ignore ( "Not a real test, just some quick hacking" )
	public void addOntoLabels () throws Exception
	{
		addOntoLabels ( "biosd_zooma_annotations" );
		addOntoLabels ( "biosd_bioportal_annotations" );
	}

	/**
	 * Takes a TSV of BioSD labels of interest (value/type strings) and annotate them with ZOOMA or Bioportal.
	 * It does so by means of the BioSD RDF exporter, since it's quick to do and we want to test how that component works.
	 */
	private void generateRDF ( String discovererProp, String inputBaseName, String outBaseName ) 
		throws Exception
	{
		log.info ( "Generating Annotations for {} - {}", discovererProp, inputBaseName );
		
		System.setProperty ( BioSdOntologyTermResolver.ONTO_DISCOVERER_PROP_NAME, discovererProp );
		InputStream in = this.getClass ().getResourceAsStream ( "/attr_cmp_test/" + inputBaseName + ".tsv" );

		// Generate a foo submission, with a foo sample
		MSI msi = new MSI ( "testMSI" );
		BioSample sample = new BioSample ( "testSmp" );
		msi.addSample ( sample );
		msi.setPublicFlag ( true );
		
		// Add our input attributes to the foo sample
		//
		CSVReader csvr = new CSVReaderBuilder ( new InputStreamReader ( in ) )
		  .withSkipLines ( 1 )
			.withCSVParser ( new CSVParserBuilder ().withSeparator ( '\t' ).build () )
			.build ();
		
		for ( String[] line = null;  ( line = csvr.readNext () ) != null;  )
		{
			String type = line [ 0 ], value = line [ 1 ];
			sample.addPropertyValue ( new ExperimentalPropertyValue<ExperimentalPropertyType> ( 
				value, new ExperimentalPropertyType ( type ) 
			));
		}
		
		// Annotate the resulting BioSD submission, a bit convoluted, but it's easy with the already-existing code.
		//
		
		BioSdRfMapperFactory.init (); // cause the definition of BioSD-specific namespaces
		
		OWLOntologyManager owlMgr = OWLManager.createOWLOntologyManager ();
		OWLOntology onto = owlMgr.createOntology ( IRI.create ( ns ( "biosd-dataset" ) ) );

		new BioSdRfMapperFactory ( onto ).map ( msi );
		
		PrefixOWLOntologyFormat fmt = new TurtleOntologyFormat ();
		for ( Entry<String, String> nse: getNamespaces ().entrySet () )
			fmt.setPrefix ( nse.getKey (), nse.getValue () );
		
		owlMgr.saveOntology ( onto, fmt, new FileOutputStream ( new File ( "target/" + outBaseName + ".ttl" ) ));
	}

	
	/** 
	 * Takes the result of {@link #generateRDF(String, String, String)} and adds up labels/synonyms of computed ontology 
	 * terms. In order to simplify that, such RDF results where first converted into TSV files, by means of loading onto
	 * a triple store (Fuseki) and then querying back attribute annotations. 
	 */
  private void addOntoLabels ( String annsResBaseName ) throws IOException
  {
  	log.info ( "Adding ontology labels to {}", annsResBaseName );
  	
		InputStream in = this.getClass ().getResourceAsStream ( "/attr_cmp_test/" + annsResBaseName + ".tsv" );

		CSVReader csvr = new CSVReaderBuilder ( new InputStreamReader ( in ) )
	  .withSkipLines ( 1 )
		.withCSVParser ( new CSVParserBuilder ().withSeparator ( '\t' ).withQuoteChar ( '"' ).build () )
		.build ();
  	
		CSVWriter csvw = new CSVWriter ( 
			new FileWriter ( "target/" + annsResBaseName + "_labels.tsv" ),
			'\t',
			'"'
		);
		csvw.writeNext ( new String[] { "type", "value", "term uri", "term label", "term synonims" } );
		
		BioportalClient bpcli = new BioportalClient ( BioSdOntologyTermResolver.BIOPORTAL_API_KEY );
		
		int lineNo = 0;
		for ( String[] line = null;  ( line = csvr.readNext () ) != null;  )
		{
			log.info ( "Line #{}", lineNo );
			
			String type = trimToEmpty ( line [ 0 ] ), 
					   value = trimToEmpty ( line [ 1 ] ), 
					   uri = trimToEmpty ( line [ 2 ] );
			
			if ( uri.startsWith ( "<" ) ) uri = uri.substring ( 1 );
			if ( uri.endsWith ( ">" ) ) uri = uri.substring ( 0, uri.length () - 1 );
			
			
			String ontoLabel = "", ontoSyns = "";
			
			OntologyClass ontoClass = null; 

			try {
				log.info ( "URI: " +  uri );
				ontoClass = bpcli.getOntologyClass ( null, uri );
			}
			catch ( OntologyServiceException ex ) {
				log.warn ( "Error on Bioportal: ", ex.getMessage (), ex );
			}
			
			if ( ontoClass != null ) 
			{
				ontoLabel = ontoClass.getPreferredLabel ();
				Set<String> syns = ontoClass.getSynonyms ();
				if ( syns != null )
					ontoSyns = StringUtils.join ( ontoClass.getSynonyms (), ", " );
			}
			
			csvw.writeNext ( new String [] { type, value, uri, ontoLabel, ontoSyns } );
			log.info ( ontoLabel );
			lineNo++;
		}
		
		csvw.close ();
  }


}
