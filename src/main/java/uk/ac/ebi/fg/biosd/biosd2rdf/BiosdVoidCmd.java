package uk.ac.ebi.fg.biosd.biosd2rdf;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

/**
 * A simple command that generates a BioSD data set VOID document, by taking a few parameters and binding placeholders
 * in a template file.
 * 
 * Syntax is: biosd_void_make.sh.sh [--template <path>] [--properties <property-file>] [output-path]
 *
 * <dl><dt>date</dt><dd>17 Jul 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class BiosdVoidCmd
{
	
	public static void main ( String[] args ) throws Throwable
	{
		int exCode = 0;
		CommandLine cli = null;
		
		try
		{
			CommandLineParser clparser = new GnuParser ();
			cli = clparser.parse ( getOptions(), args );

			if ( cli.hasOption ( 'h' ) ) throw new ParseException ( "--help" );

			args = cli.getArgs ();
			
			String tplPath = cli.getOptionValue ( 't', "./rdf/biosd_void_template.ttl" );
			String propPath = cli.getOptionValue ( 'p' );
			
			Properties props;
			if ( propPath == null ) props = System.getProperties ();
			else
			{
				props = new Properties ();
				props.load ( new FileReader ( new File ( propPath ) ) );
			}
			
			String result = StrSubstitutor.replace ( IOUtils.toString ( new FileReader ( new File ( tplPath ) ) ), props );
			PrintStream out = args.length > 0 
				? new PrintStream ( new FileOutputStream ( new File ( args [ 0 ] ) ) )
				: System.out;
			
			out.print ( result );
			out.close ();
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
			System.exit ( exCode );
		}
	}
	
	
	private static void printUsage ()
	{
		out.println ();

		out.println ( "\n\n *** BioSD RDF Exporter ***" );
		out.println ( "\nCreates a void file, merging a template with some release-specific parameters, taken from java properties." );
		
		out.println ( "Syntax:" );
		out.println ( "\n\tbiosd_void_make.sh [options] [output-path]\n\n" );

		err.println ( "\nOptions:" );
		
		HelpFormatter helpFormatter = new HelpFormatter ();
		PrintWriter pw = new PrintWriter ( err, true );
		helpFormatter.printOptions ( pw, 100, getOptions (), 2, 4 );
		
		out.println ( "\nStandard output is used if the output path is omitted.\n" );
	}
	
	@SuppressWarnings ( { "static-access" } )
	private static Options getOptions ()
	{
		Options opts = new Options ();
		
		opts.addOption ( OptionBuilder
			.withDescription ( "Template file (default is ./rdf/biosd_void_template.ttl" )
			.hasArg ().withArgName ( "<path>" )
			.withLongOpt ( "template" )
			.create ( 't' )
		);

		opts.addOption ( OptionBuilder
			.withDescription ( ".properties file containing template's placeholder values (uses -D parameters if omitted)" )
			.hasArg ().withArgName ( "<path>" )
			.withLongOpt ( "properties" )
			.create ( 'p' )
		);

		opts.addOption ( OptionBuilder
			.withDescription ( "Prints out this message" )
			.withLongOpt ( "help" )
			.create ( 'h' )
		);

		return opts;		
	}
}
