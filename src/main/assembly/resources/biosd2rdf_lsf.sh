#!/bin/sh

cd "$(dirname $0)"
MYDIR="$(pwd)"

# The output file (base name, suffixes+extension will be added when multiple files are generated, i.e., always)
if [ "$BIOSD2RDF_OUTFILE" == '' ]; then BIOSD2RDF_OUTFILE='biosd'; fi

# How many parallel LSF nodes?
if [ "$BIOSD2RDF_LSF_NODES" == '' ]; then BIOSD2RDF_LSF_NODES=3; fi

# Random sample?
if [ "$BIOSD2RDF_SAMPLE_SIZE" == '' ]; then BIOSD2RDF_SAMPLE_SIZE=100; fi

# The LSF group used for loading jobs
if [ "$BIOSD2RDF_LSF_GROUP" == '' ]; then BIOSD2RDF_LSF_GROUP='biosd2rdf'; fi


# If it doesn't already exist, create an LSF group to manage a limited running pool
if [ "$(bjgroup -s /$BIOSD2RDF_LSF_GROUP 2>&1)" == 'No job group found' ]; then
  bgadd -L $BIOSD2RDF_LSF_NODES /$BIOSD2RDF_LSF_GROUP
else
  bgmod -L $BIOSD2RDF_LSF_NODES /$BIOSD2RDF_LSF_GROUP # Just to be sure
fi



# This is absolutely necessary when you run the exporter through the cluster, since every instance of it sucks up to 
# 100 DB connections and parallel instances will soon overcome the server limit.
# ZOOMA doesn't seem to cope with more than 100 parallel requests, which have to be spread over the jobs.
#
nthreads=$(( 105 / $BIOSD2RDF_LSF_NODES ))
export OPTS="$OPTS -Duk.ac.ebi.fg.biosd.biosd2rdf.maxThreads=$nthreads" 

# Which service to use to get ontology annotations from text labels about sample properties 
#export OPTS="$OPTS -Duk.ac.ebi.fg.biosd.biosd2rdf.ontoDiscoverer=bioportal"

# This is also important with Bioportal. It doesn't accept more than 15 calls/min per each process
#min_call_delay=$(( $nthreads * 1000 / 15 ))
#min_call_delay=5000

# This parameter can be used with 'uk.ac...ontoDiscoverer=zooma' as well
#export OPTS="$OPTS -Duk.ac.ebi.fg.biosd.biosd2rdf.minCallDelay=${min_call_delay}"


# Create bsub invocations for every data chunk
# I know, there is xargs for this, but it turns out that it sucks and I cannot find a way to tell it to split arguments when I want.
# 
ct=0; chunk_size=10000; chunkct=0
(for acc in $(./biosd2rdf.sh -l -z $BIOSD2RDF_SAMPLE_SIZE )
do
	if [ $ct == 0 ]; then 
	  printf "\n"
		printf "bsub -J biosd2rdf_$chunkct -g /$BIOSD2RDF_LSF_GROUP -oo ./logs/biosd2rdf_$chunkct.out -M 24000 ./biosd2rdf.sh --output \'${BIOSD2RDF_OUTFILE}_$chunkct.ttl\' "
		((chunkct++))
	fi
	printf "$acc "
	ct=$[ ( $ct + 1 ) % $chunk_size ]
done) >bsub_$$.sh

# And then invoke
# 
sh ./bsub_$$.sh
rm ./bsub_$$.sh

# Now poll the LSF and wait until all the jobs terminate.
echo 'All the exporting jobs submitted, now waiting for their termination, please be patient.'
while [ "$(bjobs -g /$BIOSD2RDF_LSF_GROUP 2>&1)" != 'No unfinished job found' ]
do
  sleep 5m
done

echo
echo 'All Finished.'
echo
echo
