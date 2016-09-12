version=$1
if [ "$version" == "" ]; then

	cat <<EOT


	Usage: $(basename $0) <dataset-version>

Must be run after 'become bsd-svc'. Zips the contents of data/ (i.e., the RDF files uploaded on Virtuoso) to create 
a tar.bz2 file in our FTP space.

EOT

	exit 1
fi

ds_dir=/nfs/production3/linked-data/biosamples
archive_base_name=biosd_rdf_$version

# Parallel BZIP2, much faster, very cool! http://compression.ca/pbzip2
PBZIP2=/homes/bsd-svc/local/bin/pbzip2
cd "$ds_dir"
tar cv "$version" --transform="s|^$version|$archive_base_name|" \
  | $PBZIP2 --stdout >/ebi/ftp/pub/databases/biosamples/biosd2rdf/$archive_base_name.tar.bz2

echo 'The End'
