#!/bin/bash

JDK21_HOST="https://admin:La6ohgaighei@127.0.0.1:9201"
JDK17_HOST="https://admin:La6ohgaighei@127.0.0.1:9202"
MESSAGES=25
RUNS=50

long_string=$(head -c 1000000 /dev/zero | tr '\0' '\141')
tmp_file="bulk-data"

echo -n "" > $tmp_file
for i in $(seq 1 $MESSAGES); do
    echo "{ \"index\": { \"_index\": \"test\"} }" >> $tmp_file
    echo "{ \"message\":\"$long_string\"}" >> $tmp_file
done

echo 
echo "Running $RUNS bulk request of $MESSAGES large 1MB messages against 2.12.0 on JDK17:"
echo 

echo -n "Deleting test index: "; curl -k -XDELETE $JDK17_HOST/test; echo
echo -n "Creating test index (0 replicas, 1 shard): "; curl -k -XPUT $JDK17_HOST/test -d '{"settings": {"index": {"number_of_shards": 1,"number_of_replicas": 0}}}' --header "Content-Type: application/json" ; echo
for run in $(seq 1 $RUNS); do
    echo "Request $run"
    reply=$(curl -s $JDK17_HOST/_bulk -k --data-binary @$tmp_file --header "Content-Type: application/json")
    case "$reply" in 
        *"circuit:"* ) echo "$reply";;
        * ) echo "Ok";;
    esac
done

echo 
echo "Running $RUNS bulk request of $MESSAGES large 1MB messages against 2.12.0 on JDK21 (default container):"
echo
echo -n "Deleting test index: "; curl -k -XDELETE $JDK21_HOST/test ; echo
echo -n "Creating test index (0 replicas, 1 shard): "; curl -k -XPUT $JDK21_HOST/test -d '{"settings": {"index": {"number_of_shards": 1,"number_of_replicas": 0}}}' --header "Content-Type: application/json" ; echo
for run in $(seq 1 $RUNS); do
    echo "Request $run"
    reply=$(curl -s $JDK21_HOST/_bulk -k --data-binary @$tmp_file --header "Content-Type: application/json")
    case "$reply" in 
        *"circuit"* ) echo "$reply";;
        * ) echo "Ok";;
    esac
done

rm $tmp_file
