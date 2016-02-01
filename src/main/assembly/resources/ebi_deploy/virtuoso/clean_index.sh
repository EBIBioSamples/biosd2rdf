# Clean the old Virtuoso indices about BioSD  
#

# Dry-run first
cd /nfs/public/rw/homes/rdf_adm/scripts/deploy-virtuoso-opensource
./cleanup-index.pl -n biosamples -i prod --dry-run

echo 'If you are fine with thi, press [Return] to do it for real' 
read

./cleanup-index.pl -n biosamples -i prod
