#!/bin/bash

cd "$(dirname $0)"
mydir="$(pwd)"

biosd2rdf_home=/net/isilonP/public/rw/homes/rdf_adm/biosamples/biosd2rdf_cmdline

if [ "$mydir" != "$biosd2rdf_home" ]; then 
  cat <<EOT


  You must copy this script to '$biosd2rdf_home' and run it from there

EOT

  exit 1
fi

src_dir=/ebi/microarray/home/biosamples/prod/sw/biosd2rdf_cmdline/ebi_deploy

if [ '' == "$(find "$src_dir" -maxdepth 0 -newer ebi_deploy)" ]; then
  echo 'Files already up to date, exiting'
  exit 1
fi

/bin/cp -R -d --preserve=all --verbose "$src_dir" .

vdir=biosamples/biosd2rdf_cmdline/ebi_deploy
cd "$HOME/$vdir"
scp_cmd="scp -rC -i $HOME/.ssh/id_rsa -v"
$scp_cmd virtuoso rdf-pg-01:"$vdir"
$scp_cmd virtuoso rdf-oy-01:"$vdir"

echo
echo "Done."
echo
