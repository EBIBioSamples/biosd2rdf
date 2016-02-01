# Sends the data index about biosd, created after loading (load.sh) on rdf-hx-01, to the next server in the 
# publishing pipeline (rdf-hx-02), as it's documented at
# http://www.ebi.ac.uk/seqdb/confluence/display/TECHSTRATEGY/Virtuoso+infrastructure
#
# As explained in that link, this must be run from rdf-hx-01
#
cd "$(dirname $0)"
. ./_get_ver.sh

cd /nfs/public/rw/homes/rdf_adm/scripts/deploy-virtuoso-opensource
./push-index.sh -n biosamples -i prod -v $version
