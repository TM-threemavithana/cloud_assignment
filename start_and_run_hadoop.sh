#!/bin/bash
# start_and_run_hadoop.sh

source ~/.bashrc

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/.env}"
if [ ! -f "$ENV_FILE" ]; then
	echo "ERROR: Required env file not found: $ENV_FILE"
	echo "Create .env in $SCRIPT_DIR with all required variables."
	exit 1
fi

set -a
source "$ENV_FILE"
set +a

# Ensure Hadoop env vars are set for this session
: "${PROJECT_ROOT:?Missing required variable PROJECT_ROOT in $ENV_FILE}"
: "${JAVA11_HOME:?Missing required variable JAVA11_HOME in $ENV_FILE}"
: "${HADOOP_HOME:?Missing required variable HADOOP_HOME in $ENV_FILE}"

if [ -d "$JAVA11_HOME" ]; then
	export JAVA_HOME="$JAVA11_HOME"
else
	export JAVA_HOME=$(readlink -f /usr/bin/java | sed 's:bin/java::')
fi
export PATH="$JAVA_HOME/bin:$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin"

echo "Starting Hadoop cluster..."
"$HADOOP_HOME/sbin/start-dfs.sh"
"$HADOOP_HOME/sbin/start-yarn.sh"

echo "Waiting for NameNode to exit SafeMode..."
sleep 15
"$HADOOP_HOME/bin/hdfs" dfsadmin -safemode wait

echo "Running Hadoop MapReduce job script..."
"$PROJECT_ROOT/run_hadoop.sh"

echo "Stopping Hadoop cluster..."
"$HADOOP_HOME/sbin/stop-yarn.sh"
"$HADOOP_HOME/sbin/stop-dfs.sh"
