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

ct=0; chunk_size=5000; chunkct=0
(for acc in $(./biosd2rdf.sh -l -z $BIOSD2RDF_SAMPLE_SIZE )
do
	if [ $ct == 0 ]; then 
		echo -J biosd2rdf_$chunkct -g /$BIOSD2RDF_LSF_GROUP -oo "./logs/biosd2rdf_$chunkct".out -M 38000 ./biosd2rdf.sh --output "'"$BIOSD2RDF_OUTFILE"_"$chunkct".ttl'"
		chunkct=$[ $chunkct + 1 ]
	fi
	echo $acc
	ct=$[ ( $ct + 1 ) % $chunk_size ]
done) | 
	xargs -d '\n' -n $[ $chunk_size + 1] echo bsub >bsub_$$.sh

# For some damn reason, if we invoke the command above straight, bsub thinks './biosd2rdf.sh --output ...' is still an option and tries to invoke the
# first accession. I hate this job when it gets like this sh**t...
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
