# Allows you to see the Virtuoso loading progress, once started with load.sh
#
sql='select count(*) from DB.DBA.LOAD_LIST'
isql='/nfs/public/rw/homes/rdf_adm/virtuoso-opensource-7/bin/isql 127.0.0.1:1151 dba dba'

echo ------ Loaded Files ------
echo "$sql WHERE ll_state = 2;" | $isql

echo ------ Total Files ------
echo "$sql;" | $isql
