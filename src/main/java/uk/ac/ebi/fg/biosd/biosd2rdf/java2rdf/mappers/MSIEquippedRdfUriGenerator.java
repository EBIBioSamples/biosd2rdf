package uk.ac.ebi.fg.biosd.biosd2rdf.java2rdf.mappers;

import uk.ac.ebi.fg.java2rdf.mappers.RdfUriGenerator;

public abstract class MSIEquippedRdfUriGenerator<T> extends RdfUriGenerator<T>
{
	private String msiAcc; 

	public MSIEquippedRdfUriGenerator () {
		super ();
	}

	public String getMsiAcc ()
	{
		return msiAcc;
	}

	void setMsiAcc ( String msiAcc )
	{
		this.msiAcc = msiAcc;
	}
}
