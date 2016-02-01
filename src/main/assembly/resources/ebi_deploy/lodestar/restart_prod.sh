# To be run by tc_rdf01@ebi-001, restart the production servers
tomcat_dir=/nfs/public/rw/webadmin/tomcat/bases/rdf/tc-rdf-sparql
for dst in ves-hx-f1 ves-oy-f1 ves-pg-f1
do
  echo "Restarting $dst"
  ssh -Yf "$dst" "$tomcat_dir/bin/controller stop; $tomcat_dir/bin/controller start; exit"
done

