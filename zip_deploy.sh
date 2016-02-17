#!/bin/sh

# Deploys the command line binary. This doesn't do much, but it's needed with Bamboo.
# 

MYDIR=$(dirname "$0")

cd "$MYDIR"
version="$(mvn help:evaluate -Dexpression=project.version | grep -v '\[')"

cd target

target="$1"

if [ "$target" == "" ]; then
  target=.
fi;

echo
echo 
echo "_______________ Deploying the Command Line Binary ($version) to $target _________________"

# We need to remove old versions and unused libs
rm -Rf "$target/biosd2rdf_cmdline_${version}/{lib,rdf}"

yes A| unzip biosd2rdf_cmdline_${version}.zip -d "$target"
cp -f biosd2rdf_cmdline_${version}.zip "$target"

# TODO: I know, I know, that's not good, but we have issues at EBI, like multiple users needing to access this
# In future we'll add a check to understand when we are within our organisation. 
chmod -R ugo=rwX "$target/biosd2rdf_cmdline_${version}" 

echo ______________________________________________________________________________
echo
echo
echo
