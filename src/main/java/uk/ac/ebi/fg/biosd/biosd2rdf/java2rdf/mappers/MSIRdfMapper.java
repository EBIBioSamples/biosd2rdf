/*
 * 
 */
package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import static uk.ac.ebi.fg.java2rdf.utils.NamespaceUtils.ns;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.java2rdf.mappers.BeanRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.CollectionPropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.RdfUriGenerator;
import uk.ac.ebi.fg.java2rdf.mappers.ToDatatypePropRdfMapper;
import uk.ac.ebi.fg.java2rdf.mappers.ToObjectPropRdfMapper;

/**
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>Apr 23, 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public class MSIRdfMapper extends BeanRdfMapper<MSI>
{
	public MSIRdfMapper ()
	{
		super ( 
			ns ( "biosd", "Submission" ), 
			new RdfUriGenerator<MSI>() {
			@Override
			public String getUri ( MSI msi ) {
				return ns ( "biosd", "msi/" + msi.getAcc () );
			}
		});
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<MSI, String> ( "title", ns ( "dc", "title" ) ) );
		this.setPropertyMapper ( new ToDatatypePropRdfMapper<MSI, String> ( "description", ns ( "dc", "description" ) ) );
		// TODO: more
		
		this.setPropertyMapper ( new CollectionPropRdfMapper<MSI, BioSample> ( 
			"samples", ns ( "obo", "IAO_0000219" ), new ToObjectPropRdfMapper<MSI, BioSample> () ) // denotes
		);
	}

}
