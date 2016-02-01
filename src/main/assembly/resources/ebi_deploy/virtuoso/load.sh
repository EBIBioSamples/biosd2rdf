# Loads BioSD data onto Virtuoso, issuing the steps documented at 
# http://www.ebi.ac.uk/seqdb/confluence/display/TECHSTRATEGY/Virtuoso+infrastructure
#
# As explained in that link, this must be run from rdf-hx-01
#

cd "$(dirname $0)"
. ./_get_ver.sh

cd ../..
mydir="$(pwd)"

ds_dir=/nfs/production2/linked-data/biosamples/$version

cd /nfs/public/rw/homes/rdf_adm/scripts/load-virtuoso-opensource

echo clean
./virtuoso-clean.sh -n biosamples -i prod -v $version
echo 'Press [Return] to continue'
read

# We don't do prepare-data.sh (step 1), since usually there isn't enough room on /source 
echo load-prepare
./virtuoso-load-prepare.sh -n biosamples -i prod -v $version "$ds_dir"
echo 'Press [Return] to continue'
read


out_file="$mydir"/logs/virtuoso_load.out

echo load-prepare
nohup ./virtuoso-load-start.sh -n biosamples -i prod -v $version -r &>"$out_file" &

echo Should have been launched. Check progress in $out_file, or via: ./load_check.sh
