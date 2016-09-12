version=$1
if [ "$version" == "" ]; then

	cat <<EOT


	Usage: $(basename $0) <dataset-version>

Must be run after 'become bsd-svc'. Zips the contents of data/ (i.e., the RDF files uploaded on Virtuoso) to create 
a tar.bz2 file in our FTP space.

EOT

	exit 1
fi

archive_base_name=biosd_rdf_$version

# Parallel BZIP2, much faster, very cool! http://compression.ca/pbzip2
PBZIP2="$HOME/local/bin/pbzip2"
FTP_DIR=/ebi/ftp/pub/databases/biosamples/biosd2rdf
cd /nfs/production3/linked-data/biosamples
tar cv $version --transform="s|^data|$archive_base_name|" \
  | "$PBZIP2" --stdout >"$FTP_DIR/$archive_base_name.tar.bz2"

echo "OK, now consider to remove something at '$FTP_DIR'"
echo 'The End'
