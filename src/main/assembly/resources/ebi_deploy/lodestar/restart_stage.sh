# To be run by tc_rdf01@ebi-001, restart the stage server
tomcat_dir=/nfs/public/rw/webadmin/tomcat/bases/rdf/tc-rdf-sparql_dev
ssh -Yf ves-hx-f2 "$tomcat_dir/bin/controller stop; $tomcat_dir/bin/controller start; exit"

