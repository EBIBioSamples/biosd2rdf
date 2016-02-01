#
# Invokes a SPARQL endpoint with the ASK query in the standard input and expects a TRUE result to be
# returned, exits with 0 if that's the case, 1 if not.
#  
# It also spits out a lot of diagnostic output do 2>/dev/null if you don't want to see it.
#
server_base="$1"; : ${server_base:=http://www.ebi.ac.uk/rdf/services/biosamples/sparql}
# Get the query from stdin, strip comments away, prepend common prefixes
query="$(cat ./prefixes.sparql) $(cat | sed s/'^\b*\#.*'// | sed s/'#.*$'//)"
query="$(echo $query)" # Remove \n and make it one line
echo $query >&2
#wget -O - "http://www.ebi.ac.uk/rdf/services/biosamples/sparql?query=$query&format=TSV"
out=$(curl --data-urlencode "query=$query" $server_base)
echo $out >&2
echo $out | grep -q 'true'
