//package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;
//
//
//
//import org.apache.commons.lang.StringUtils;
//
//import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
//import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
//import uk.ac.ebi.fg.core_model.expgraph.properties.Unit;
//import uk.ac.ebi.fg.core_model.expgraph.properties.UnitDimension;
//import uk.ac.ebi.fg.java2rdf.mappers.RdfUriGenerator;
//import uk.ac.ebi.fg.java2rdf.utils.Java2RdfUtils;
//import uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils;
//
///**
// * TODO: Comment me!
// *
// * <dl><dt>date</dt><dd>Apr 30, 2013</dd></dl>
// * @author Marco Brandizi
// *
// */
//@SuppressWarnings ( "rawtypes" )
//public class ExpPropValUriGenerator extends RdfUriGenerator<ExperimentalPropertyValue>
//{
//	@Override
//	public String getUri ( ExperimentalPropertyValue pval )
//	{
//		if ( pval == null ) return null;
//		String valTxt = StringUtils.trimToNull ( pval.getTermText () );
//		if ( valTxt == null ) return null;
//		
//		String pvalSig = valTxt;
//		
//		ExperimentalPropertyType ptype = pval.getType ();
//		if ( ptype == null ) return null;
//		
//		String typeTxt = StringUtils.trimToNull ( ptype.getTermText () );
//		if ( typeTxt == null ) return null;
//		pvalSig += typeTxt;
//		
//		Unit unit = pval.getUnit ();
//		if ( unit != null ) 
//		{
//			String uSig = StringUtils.trimToNull ( unit.getTermText () );
//			if ( uSig != null )
//			{
//				UnitDimension udim = unit.getDimension ();
//				if ( udim != null ) {
//					String uDimSig = StringUtils.trimToNull ( udim.getTermText () );
//					if ( uDimSig != null ) { 
//						uSig += uDimSig;
//						pvalSig += uSig;
//					}
//				}
//			}
//		} 
//		
//		return NamespaceUtils.ns ( "biosd", "exp-prop-val/" + Java2RdfUtils.hashUriSignature ( pvalSig ) );
//	}
//
//}
