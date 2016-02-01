# Launches the LSF version of the BioSD-RDF exporter under the EBI infrastructure (eg, sends data to 
# proper paths).
#

version=$1
if [ "$version" == '' ]; then
  cat <<EOT
  
	usage: $0 <dataset version> [--clean <prev-version> <creation date YYYY-MM-DD>]
	
EOT
exit 1
fi

cd "$(dirname $0)/../.."

ds_dir=/nfs/production2/linked-data/biosamples/$version
if ! [ -e "$ds_dir" ]; then 
  mkdir "$ds_dir"
fi

clean_opt=$2
prev_ver=$3
creation_date=$4

if [ "$clean_opt" == '--clean' ]; then
  if [ "$prev_ver" == '' -o "$creation_date" == '' ]; then
    cat <<EOT
    
	Need <prev-version> after --clean (to generate the VOID file)

EOT
  exit 1
  fi

	# In case of --clean, we remove data from the data dir, copy ontologies on it, create the VOID file.
	#
  rm -f logs/* "$ds_dir/*"
  /bin/cp -R -d --preserve=all --verbose ./rdf/biosd_terms.ttl ./rdf/ext/* "$ds_dir" 

  export OPTS="-Dbiosd.version=$version -Dbiosd.previous-version=$prev_ver -Dbiosd.creation-date=$creation_date"
  ./biosd_void_make.sh >"$ds_dir/void.ttl"
fi


export BIOSD2RDF_OUTFILE="$ds_dir/biosd"
#export BIOSD2RDF_OUTFILE='/data/biosd'
#export BIOSD2RDF_LSF_NODES=3

# Change the dev ZOOMA server, rather than production
#export OPTS="$OPTS -Duk.ac.ebi.fg.biosd.biosd2rdf.zooma.apiurl=http://wwwdev.ebi.ac.uk/fgpt/zooma"
#export OPTS="$OPTS -Duk.ac.ebi.fg.biosd.biosd2rdf.zooma.apiurl=http://ebi-001.ebi.ac.uk:8180/zooma"

# Experimental, use both annotators, or zooma-first (default is zooma, another option is bioportal)
#export OPTS=$OPTS '-Duk.ac.ebi.fg.biosd.biosd2rdf.ontoDiscoverer=both'

nohup ./biosd2rdf_lsf.sh &>logs/lsf.out &
