cd "$(dirname $0)"
mydir="$(pwd)"


src_dir=/ebi/microarray/home/biosamples/prod/sw/biosd2rdf_cmdline/ebi_deploy

if [ '' == "$(find "$src_dir/virtuoso" -maxdepth 0 -mmin -15)" ]; then
  echo 'Files already up to date, exiting'
  exit 1
fi

cd ..
myupdir="$(pwd)"

scp -rC virtuoso rdf-pg-01:"$myupdir"
scp -rC virtuoso rdf-oy-01:"$myupdir"

echo
echo "Done."
echo
