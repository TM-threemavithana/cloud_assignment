#!/bin/bash
# run_hadoop.sh — Java MapReduce version

source ~/.bashrc

HADOOP_BIN="/usr/local/hadoop/bin/hadoop"
HDFS_BIN="/usr/local/hadoop/bin/hdfs"
JAR_PATH="/mnt/c/Users/User/cloud_assignment/crypto-analysis.jar"

# 1. Clean up HDFS
echo "Cleaning up previous HDFS directories..."
$HDFS_BIN dfs -rm -r -skipTrash /crypto_input 2>/dev/null
$HDFS_BIN dfs -rm -r -skipTrash /crypto_output 2>/dev/null

# 2. Make input directory and upload dataset
echo "Uploading dataset to HDFS..."
$HDFS_BIN dfs -mkdir -p /crypto_input
$HDFS_BIN dfs -put /mnt/c/Users/User/cloud_assignment/data/top_100_cryptos_with_correct_network.csv /crypto_input/

# 3. Run the MapReduce Job using our compiled JAR
echo "Starting Hadoop MapReduce Job (Java)..."

$HADOOP_BIN jar $JAR_PATH CryptoDriver \
    /crypto_input/top_100_cryptos_with_correct_network.csv \
    /crypto_output

if [ $? -eq 0 ]; then
    echo "MapReduce Job Completed Successfully!"

    # 4. Retrieve output
    echo "Retrieving output from HDFS..."
    rm -rf /mnt/c/Users/User/cloud_assignment/hadoop_output
    mkdir -p /mnt/c/Users/User/cloud_assignment/hadoop_output
    $HDFS_BIN dfs -get /crypto_output/* /mnt/c/Users/User/cloud_assignment/hadoop_output/

    echo "Done! Output saved to cloud_assignment/hadoop_output/"
else
    echo "MapReduce Job Failed."
fi
