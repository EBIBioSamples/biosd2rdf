#!/bin/sh

# This is the entry point that invokes the BioSD-VOID creation line command.
# 

# Do you use a proxy?
if [ "$http_proxy" != '' ]; then
  OPTS="$OPTS -DproxySet=true -DproxyHost=wwwcache.ebi.ac.uk -DproxyPort=3128 -DnonProxyHosts='*.ebi.ac.uk|localhost'"
fi

# These are passed to the JVM. they're appended, so that you can predefine your stuff via export OPTS=...
# on my laptop: OPTS="$OPTS -Xms4G -Xmx8G -XX:PermSize=256m -XX:MaxPermSize=512m"
#OPTS="$OPTS -Xms16G -Xmx32G -XX:PermSize=512m -XX:MaxPermSize=1G"

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


# You shouldn't need to change the rest
#
###

cd "$(dirname $0)"
MYDIR="$(pwd)"


# Define your classpath, eg. with the JDBC driver, if you need to include stuff 
export CLASSPATH="$CLASSPATH:$MYDIR:$MYDIR/lib/*"



# See here for an explanation about ${1+"$@"} :
# http://stackoverflow.com/questions/743454/space-in-java-command-line-arguments 

java $OPTS uk.ac.ebi.fg.biosd.biosd2rdf.BiosdVoidCmd ${1+"$@"}

EXCODE=$?

echo Java Finished. Quitting the Shell Too. >&2
exit $EXCODE
