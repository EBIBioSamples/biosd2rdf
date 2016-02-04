# Do not invoke me indirectly, I'm used by scripts needing to get the version parameter from the command line
#
version=$1
if [ "$version" == '' ]; then
  cat <<EOT

  usage: $0 <dataset version> 

EOT
exit 1
fi


