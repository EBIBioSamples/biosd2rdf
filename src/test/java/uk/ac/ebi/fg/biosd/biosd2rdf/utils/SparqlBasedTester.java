package uk.ac.ebi.fg.biosd.biosd2rdf.utils;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;

/**
 * A simple helper to verify what a triple store contains.
 *
 * <dl><dt>date</dt><dd>7 Oct 2014</dd></dl>
 * @author Marco Brandizi
 *
 */
public class SparqlBasedTester
{
	private Model model;
	private String sparqlPrefixes = "";
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	/**
	 * @param url the path to the RDF source to be tested.
	 * @param sparqlPrefixes SPARQL prefixes that are used in queries
	 */
	public SparqlBasedTester ( String url, String sparqlPrefixes )
	{
		model = ModelFactory.createDefaultModel ();
		model.read ( url );
		if ( this.sparqlPrefixes != null )
			this.sparqlPrefixes = sparqlPrefixes;
	}
	
	/**
	 * No prefix defined.
	 */
	public SparqlBasedTester ( String url )
	{
		this ( url, null );
	}

	
	/**
	 * Performs a SPARQL ASK test and asserts via JUnit that the result is true.
	 *  
	 * @param errorMessage the error message to report in case of failure
	 * @param sparql the SPARQL/ASK query to run against the triple store passed to the class constructor.
	 */
	public void testRDFOutput ( String errorMessage, String sparql )
	{
		sparql = sparqlPrefixes + sparql;
		try
		{
			Query q = QueryFactory.create ( sparql );
			QueryExecution qe = QueryExecutionFactory.create ( q, model );
			Assert.assertTrue ( errorMessage, qe.execAsk () );
		}
		catch ( JenaException ex ) 
		{
			log.error ( "Error while doing SPARQL, query is: {}, error is: {}", sparql, ex.getMessage () );
			throw ex;
		}
	}
}
