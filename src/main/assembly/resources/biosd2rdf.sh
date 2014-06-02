#!/bin/sh

# This is the entry point that invokes the BioSD-RDF line command.
# 

# Do you use a proxy? TODO: this only works at EBI, we need to parse $http_proxy
if [ "$http_proxy" != '' ]; then
	for proto in http https; do
	  OPTS="$OPTS -D$proto.proxyHost=wwwcache.ebi.ac.uk -D$proto.proxyPort=3128 -D$proto.nonProxyHosts='*.ebi.ac.uk|localhost'"
	done	
fi

# These are passed to the JVM. they're appended, so that you can predefine your stuff via export OPTS=...
# on my laptop: OPTS="$OPTS -Xms4G -Xmx8G -XX:PermSize=256m -XX:MaxPermSize=512m"
# OPTS="$OPTS -Xms16G -Xmx32G -XX:PermSize=512m -XX:MaxPermSize=1G"
OPTS="$OPTS -Xms12G -Xmx24G -XX:PermSize=512m -XX:MaxPermSize=1G"

# Sometimes it hangs on Zooma, this will make it to timeout (they are -1 = oo by default)
OPTS="$OPTS -Dsun.net.client.defaultConnectTimeout=30000 -Dsun.net.client.defaultReadTimeout=120000"

# We always work with universal text encoding.
OPTS="$OPTS -Dfile.encoding=UTF-8"

# Monitoring with jconsole (end-user doesn't usually need this)
#OPTS="$OPTS 
# -Dcom.sun.management.jmxremote.port=5010
# -Dcom.sun.management.jmxremote.authenticate=false
# -Dcom.sun.management.jmxremote.ssl=false"
       
# Used for invoking a command in debug mode (end user doesn't usually need this)
#OPTS="$OPTS -Xdebug -Xnoagent"
#OPTS="$OPTS -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

# The Database driver. You need to set this to your driver, in case you don't use one of the provided ones
# 
#JDBCPATH=/path/to/jdbc_driver.jar

# You shouldn't need to change the rest
#
###

cd "$(dirname $0)"
MYDIR="$(pwd)"

CP="$MYDIR:$MYDIR/${project.artifactId}_deps.jar"
if [ "$JDBCPATH" != "" ]; then
  CP="$CP:$JDBCPATH"
fi

# See here for an explaination about ${1+"$@"} :
# http://stackoverflow.com/questions/743454/space-in-java-command-line-arguments 

java \
  $OPTS -cp $CP uk.ac.ebi.fg.biosd.biosd2rdf.Biosd2RdfCmd ${1+"$@"}

EXCODE=$?

echo Java Finished. Quitting the Shell Too. >&2
exit $EXCODE
