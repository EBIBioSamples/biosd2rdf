# Must be run as rdf_adm from ebi-001, after the copy of biosamples.war onto ebi-001 (see cp_ebi-001.sh) 
# Deploys the ware over the production servers
#
for dst in ves-hx-f1 ves-oy-f1 ves-pg-f1
do
  scp /tmp/biosamples.war $dst:/nfs/public/rw/webadmin/tomcat/bases/rdf/tc-rdf-sparql/deploy/biosamples.war
done
