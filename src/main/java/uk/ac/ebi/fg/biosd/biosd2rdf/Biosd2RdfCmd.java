package uk.ac.ebi.fg.biosd.biosd2rdf;

import static java.lang.System.err;
import static java.lang.System.out;

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
		BioSdExportService exportService = null; 
		CommandLine cli = null;
		
		try
		{
			CommandLineParser clparser = new GnuParser ();
			cli = clparser.parse ( getOptions(), args );
			
			if ( cli.hasOption ( 'h' ) ) throw new ParseException ( "--help" );
			
			args = cli.getArgs ();
			
			if ( args.length > 0 )
				throw new UnsupportedOperationException ( "The case of specified accessions is not supported yet" );
			
			double sampleSize = cli.hasOption ( 'z' ) ? Double.parseDouble ( cli.getOptionValue ( 'z' ) ) : 100d;

			exportService = new BioSdExportService ( cli.getOptionValue ( 'o' ) );
			
			exportService.submitAll ( sampleSize );
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
			if ( exCode != 128 ) 
			{
				EntityManagerFactory emf = Resources.getInstance ().getEntityManagerFactory ();
				if ( emf != null && emf.isOpen () ) emf.close ();

				if ( exportService != null ) exportService.flushKnowledgeBase ();
			
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
		
		opts.addOption ( OptionBuilder
			.withDescription ( "Prints out this message" )
			.withLongOpt ( "help" )
			.create ( 'h' )
		);
		
		return opts;		
	}
}
