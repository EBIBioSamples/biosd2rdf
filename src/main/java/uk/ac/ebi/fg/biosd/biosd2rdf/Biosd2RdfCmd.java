package uk.ac.ebi.fg.biosd.biosd2rdf;

import static java.lang.System.err;

import java.io.PrintWriter;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import uk.ac.ebi.fg.biosd.biosd2rdf.threading.BioSdExportService;
import uk.ac.ebi.fg.core_model.resources.Resources;

/**
 * The entry point to the BioSD RDF exporter command line.
 * For the moment, it only supports exports from the BioSD relational database, all the submissions, or a random subset.
 *
 * <dl><dt>date</dt><dd>17 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class Biosd2RdfCmd
{
	
	public static void main ( String... args )
	{
		int exCode = 0;
		BioSdExportService exportService = null; 
		CommandLine cli = null;
		
		try
		{
			CommandLineParser clparser = new GnuParser ();
			cli = clparser.parse ( getOptions(), args );
			
			if ( cli.hasOption ( 'h' ) ) throw new ParseException ( "--help" );

			exportService = new BioSdExportService ( cli.getOptionValue ( 'o' ) );
			
			
			args = cli.getArgs ();
			
			if ( args.length > 0 )
				// Accessions of submissions to be exported come from the param
				exportService.submitAll ( args );
			else
			{
				// Or, we pick a random subset of them, or all of them. 
				//
				double sampleSize = cli.hasOption ( 'z' ) ? Double.parseDouble ( cli.getOptionValue ( 'z' ) ) : 100d;

				// submit export tasks to the thread pool executor. Here we will be put on wait when all the pool threads are
				// busy, i.e., we'll wait most of time, until the tail of the submission table
				exportService.submitAll ( sampleSize );
			}
			
			// Now that all is submitted, wait that all the export tasks complete.
			exportService.waitAllFinished ();
		} 
		catch ( Throwable ex ) 
		{
			if ( ex instanceof ParseException ) 
			{
				if ( !"--help".equals ( ex.getMessage () ) )
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
			// JUnit tests needs to set-up this if they don't want to be screwed by brutal killing 
			// of resources
			boolean isTestingMode = "true".equals ( System.getProperty ( "biosd.test_mode" ) );
			
			if ( exCode != 128 ) 
			{
				if ( !isTestingMode ) 
				{
					EntityManagerFactory emf = Resources.getInstance ().getEntityManagerFactory ();
					if ( emf != null && emf.isOpen () ) emf.close ();
				}
				
				// Saves the export results in memory, either the unique chunk available, or the last one
				if ( exportService != null ) exportService.flushKnowledgeBase ();
			
			}// exitCode
			
			// This is needed to prevent JUnit to fail.
			if ( !isTestingMode )
				System.exit ( exCode );
		}
	}
	
	
	private static void printUsage ()
	{
		err.println ();

		err.println ( "\n\n *** BioSD RDF Exporter ***" );
		err.println ( "\nExports from the BioSD relational database to RDF files." );
		
		err.println ( "Syntax:" );
		err.println ( "\n\tbiosd2rdf.sh [options] msi-acc...\n\n" );

		err.println ( "\nOptions:" );
		
		HelpFormatter helpFormatter = new HelpFormatter ();
		PrintWriter pw = new PrintWriter ( err, true );
		helpFormatter.printOptions ( pw, 100, getOptions (), 2, 4 );
		
		err.println ( "\nSee also hibernate.properites for the configuration of the target database.\n" );
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
		
		opts.addOption ( OptionBuilder
			.withDescription ( "Prints out this message" )
			.withLongOpt ( "help" )
			.create ( 'h' )
		);
		
		return opts;		
	}
}
