# This is run from the crontab of rdf-hx-01, it periodically checks wether the scripts sitting in 
# the same directory I am were changed and, if yes, it updates the copy we have on production servers.
# we don't know a different way to do this automatically.
# 
# This update action is usually triggered by zip_deploy.sh 
# (https://github.com/EBIBioSamples/biosd2rdf/blob/master/zip_deploy.sh)
#
cd "$(dirname $0)"
mydir="$(pwd)"

cd ..
find virtuoso -maxdepth 0 -mmin -15

if [ "" == "$(find virtuoso -maxdepth 0 -mmin -15)" ]; then
  echo 'Files already up to date, exiting'
  exit 1
fi

scp -rC virtuoso rdf-pg-01:"$myupdir"
scp -rC virtuoso rdf-oy-01:"$myupdir"

echo
echo "Done."
echo
