# Used by brandizi from his laptop, copies the just-deployed war onto an EBI host
scp \
  -o proxycommand="ssh -C -o tisauthentication\=yes gate.ebi.ac.uk proxy %h" \
  /Users/brandizi/Documents/Work/ebi/ebi2rdf/git/lodestar/web-ui/target/lodestar.war ebi-001:/tmp/biosamples.war
