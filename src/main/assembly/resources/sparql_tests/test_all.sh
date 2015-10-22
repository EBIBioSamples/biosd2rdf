#
# Performs all the SPARQL tests in this directory, against all the EBI endpoints
#
version="$1"
if [ "$version" == "" ]; then
  printf "\n\n\tUsage: $(basename $0) <dataset version>\n\n"
  exit 2
fi

success=0
for server in \
  http://rdf-hx-01.ebi.ac.uk:8151/sparql\
  http://rdf-hx-02.ebi.ac.uk:8151/sparql\
  http://rdf-pg-01.ebi.ac.uk:8151/sparql\
  http://rdf-oy-01.ebi.ac.uk:8151/sparql\
  http://www.ebi.ac.uk/rdf/services/biosamples/sparql
do
  for query in \
    void_graph.sparql\
    $(find tests -name '*.sparql')\
    $(find old_schema_tests -name '*.sparql')
  do
    if [ "$query" == 'void_graph.sparql' ]; then
      sed s/'\$version'/$version/g $query | ./sparql_ask.sh $server 2>/dev/null
    else
      cat $query | ./sparql_ask.sh $server 2>/dev/null
    fi
    result=$?
    printf "%s\t%s\t%s\n" $server $query $( [ $result == 0 ] && echo OK || echo FAIL )
    [ $result == 0 ] || success=1
  done
done

echo
echo
echo '-------------'  $( [ $success == 0 ] && echo SUCCESS || echo FAILURE ) '-------------'
echo 

exit $success
