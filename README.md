#BioSD Converter to RDF

It exports the [EBI Biosamples Database](http://www.ebi.ac.uk/biosamples/) into RDF files, to produce our [Linked Data end point](http://www.ebi.ac.uk/rdf/documentation/biosamples) about BioSD.

The code is mainly based on three components: 

* our [java2RDF](https://github.com/EBIBioSamples/java2rdf/) library (in addition to [ZOOMA](http://www.ebi.ac.uk/fgpt/zooma/docs/) and [bioportal_client](https://github.com/biosemantics/bioportal-client)). Read more [here](http://www.marcobrandizi.info/mysite/node/153).

* the BioSD Feature Annotator (BioSDFA), which extracts ontology and numerical information from textual values in the BioSD submitted
data

* BioSDFA, in turn, is based on [Bioportal](https://github.com/EBIBioSamples/bioportal-client) and [ZOOMA](https://github.com/EBIBioSamples/zooma), two services for ontology look-up and text/ontology mapping.

