# Formats the output of test_all.sh, as a pretty-printed table and colouring the failed test lines.
# This works on consoles/terminals only, if you need to reopen this output as CSV, use test_all.sh alone
# 
# This requires the column command
#
RED=$(echo -e '\033[0;31m')
BW=$(echo -e '\033[0m')
./test_all.sh $1 | column -c 120 -s $'\t' -tx | sed -E s/'(.*)FAIL$'/"${RED}\1FAIL${BW}"/
