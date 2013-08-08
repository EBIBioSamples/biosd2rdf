package uk.ac.ebi.fg.biosd.biosd2rdf;

import static java.lang.System.err;
import static java.lang.System.out;
import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.getNamespaces;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map.Entry;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import uk.ac.ebi.fg.biosd.biosd2rdf.threading.BioSdExportService;
import uk.ac.ebi.fg.core_model.resources.Resources;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>17 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class Biosd2RdfCmd
{
	
	public static void main ( String[] args ) throws Throwable
	{
		int exCode = 0;
		BioSdExportService exportService = new BioSdExportService ();
		CommandLine cli = null;
		
		try
		{
			CommandLineParser clparser = new GnuParser ();
			cli = clparser.parse ( getOptions(), args );
			
			args = cli.getArgs ();
			if ( args.length > 0 )
				throw new UnsupportedOperationException ( "The case of specified accessions is not supported yet" );
			
			double sampleSize = cli.hasOption ( 'z' ) ? Double.parseDouble ( cli.getOptionValue ( 'z' ) ) : 100d;

			exportService.submitAll ( sampleSize );
			exportService.waitAllFinished ();
		} 
		catch ( Throwable ex ) 
		{
			if ( ex instanceof ParseException ) 
			{
				err.println ( "\n  Command syntax error: " + ex.getMessage () + "\n\n");
				printUsage ();
				exCode = 128;
			}
			else 
			{
				ex.printStackTrace( System.err );
				exCode = 1; // TODO: proper exit code management
			}
		}
		finally 
		{
			if ( exCode != 128 ) 
			{
				EntityManagerFactory emf = Resources.getInstance ().getEntityManagerFactory ();
				if ( emf != null && emf.isOpen () ) emf.close ();

				if ( exportService != null )
				{
					OWLOntology onto = exportService.getKnolwedgeBase ();
					if ( onto != null )
					{
						err.println ( "Saving the generated knowledge base, you may have to wait still for a while..." );
						OutputStream out = cli != null && cli.hasOption ( 'o' ) 
							? new BufferedOutputStream ( new FileOutputStream ( new File ( cli.getOptionValue ( 'o' ) ) ) )
						  : System.out;
						PrefixOWLOntologyFormat fmt = new RDFXMLOntologyFormat ();
						for ( Entry<String, String> nse: getNamespaces ().entrySet () )
							fmt.setPrefix ( nse.getKey (), nse.getValue () );
						onto.getOWLOntologyManager ().saveOntology ( exportService.getKnolwedgeBase (), fmt, out );
					}
				}
			
			}// exitCode	
			System.exit ( exCode );
		}
	}
	
	
	private static void printUsage ()
	{
		out.println ();

		out.println ( "\n\n *** BioSD RDF Exporter ***" );
		out.println ( "\nExports from the BioSD relational database to RDF files." );
		
		out.println ( "Syntax:" );
		out.println ( "\n\tbiosd2rdf.sh [options] msi-acc...\n\n" );

		err.println ( "\nOptions:" );
		
		HelpFormatter helpFormatter = new HelpFormatter ();
		PrintWriter pw = new PrintWriter ( err, true );
		helpFormatter.printOptions ( pw, 100, getOptions (), 2, 4 );
		
		out.println ( "\nSee also hibernate.properites for the configuration of the target database.\n" );
	}
	
	@SuppressWarnings ( { "static-access" } )
	private static Options getOptions ()
	{
		Options opts = new Options ();
		
		opts.addOption ( OptionBuilder
			.withDescription ( "Output file path (default = stdout)." )
			.hasArg ().withArgName ( "path" )
			.withLongOpt ( "output" )
			.create ( 'o' )
		);
		
		opts.addOption ( OptionBuilder
			.withDescription ( "Exports a random subset of all the submissions in the database, taking the specified percentage." )
			.hasArg ().withArgName ( "0-100" )
			.withType ( Double.class )
			.withLongOpt ( "sample-size" )
			.create ( 'z' )
		);
		
		return opts;		
	}
}
