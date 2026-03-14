#!/bin/bash
# start_and_run_hadoop.sh

source ~/.bashrc

echo "Starting Hadoop cluster..."
/usr/local/hadoop/sbin/start-dfs.sh
/usr/local/hadoop/sbin/start-yarn.sh

echo "Waiting for NameNode to exit SafeMode..."
sleep 15
/usr/local/hadoop/bin/hdfs dfsadmin -safemode wait

echo "Running Hadoop MapReduce job script..."
/mnt/c/Users/User/cloud_assignment/run_hadoop.sh

echo "Stopping Hadoop cluster..."
/usr/local/hadoop/sbin/stop-yarn.sh
/usr/local/hadoop/sbin/stop-dfs.sh
