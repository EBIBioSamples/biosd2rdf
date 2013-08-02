//package uk.ac.ebi.fg.ontodiscover;
//
//import java.net.URI;
//import java.util.Collection;
//
//import org.springframework.beans.factory.BeanFactory;
//import org.springframework.context.support.ClassPathXmlApplicationContext;
//
//import uk.ac.ebi.microarray.zooma.eval.BadOutcomeException;
//import uk.ac.ebi.microarray.zooma.eval.OntologyMappingOutcome;
//import uk.ac.ebi.microarray.zooma.hypothesis.OntologyMappingHypothesis;
//import uk.ac.ebi.microarray.zooma.mapping.TextReportingMapper;
//import uk.ac.ebi.microarray.zooma.utils.ValueFactory;
//import uk.ac.ebi.ontocat.OntologyTerm;
//
///**
// * TODO: Comment me!
// *
// * <dl><dt>date</dt><dd>May 27, 2013</dd></dl>
// * @author Marco Brandizi
// *
// */
//public class Zooma1OntoTermDiscoverer extends OntologyTermDiscoverer
//{
//
//	@Override
//	public URI getOntologyTermUri ( String label ) throws OntologyDiscoveryException
//	{
//	  try 
//	  {
////TODO: DEBUG
//if ( false ) return null;
//	  	
//			BeanFactory factory = new ClassPathXmlApplicationContext ( "zooma-text.xml" );
//		  TextReportingMapper mapper = factory.getBean ( "textMapper", TextReportingMapper.class );
//		  //mapper.addRetriever ( new OntocatRetriever () );
//		  ValueFactory vfactory = factory.getBean ( "valueFactory", ValueFactory.class );
//			OntologyMappingOutcome outcome = mapper.generateOutcome ( vfactory.generateValue ( label ) );
//			Collection<OntologyMappingHypothesis> hypotheses = outcome.getBestHypotheses ();
//			// TODO: is it best/safest this or .size () == 1 ?!
//			if ( hypotheses != null && !hypotheses.isEmpty () ) {
//				OntologyMappingHypothesis hypo = hypotheses.iterator ().next ();
//				OntologyTerm ot = hypo.getOntologyTerms ().iterator ().next ();
//				return ot.getURI ();
//			}
//			return null;
//		} 
//	  catch ( BadOutcomeException ex ) {
//	  	throw new OntologyDiscoveryException ( "Internal error with Zooma: " + ex.getMessage (), ex );
//		}
//	}
//
//}
