# Deploys BioSD using an index pulled from a server preceding the current one in the EBI publishing
# pipeline, as it's documented at:
# http://www.ebi.ac.uk/seqdb/confluence/display/TECHSTRATEGY/Virtuoso+infrastructure
#
# As explained in that link, this should not be run from rdf-hx-01
#
 
cd "$(dirname $0)"
. ./_get_ver.sh

cd /nfs/public/rw/homes/rdf_adm/scripts/deploy-virtuoso-opensource
./virtuoso-deploy.sh -n biosamples -i prod -v $version
