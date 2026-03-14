
#!/bin/bash

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

: "${PROJECT_ROOT:?Missing required variable PROJECT_ROOT in $ENV_FILE}"
: "${JAVA11_HOME:?Missing required variable JAVA11_HOME in $ENV_FILE}"
: "${HADOOP_HOME:?Missing required variable HADOOP_HOME in $ENV_FILE}"
: "${INPUT_CSV:?Missing required variable INPUT_CSV in $ENV_FILE}"
: "${LOCAL_OUTPUT_DIR:?Missing required variable LOCAL_OUTPUT_DIR in $ENV_FILE}"
: "${LOCAL_TOPK_OUTPUT_DIR:?Missing required variable LOCAL_TOPK_OUTPUT_DIR in $ENV_FILE}"
: "${JAR_PATH:?Missing required variable JAR_PATH in $ENV_FILE}"
: "${HDFS_INPUT_DIR:?Missing required variable HDFS_INPUT_DIR in $ENV_FILE}"
: "${HDFS_OUTPUT_DIR:?Missing required variable HDFS_OUTPUT_DIR in $ENV_FILE}"
: "${HDFS_TOPK_OUTPUT_DIR:?Missing required variable HDFS_TOPK_OUTPUT_DIR in $ENV_FILE}"
: "${TOPK_K:?Missing required variable TOPK_K in $ENV_FILE}"

if [ -d "$JAVA11_HOME" ]; then
    export JAVA_HOME="$JAVA11_HOME"
else
    export JAVA_HOME=$(readlink -f /usr/bin/java | sed 's:bin/java::')
fi
export PATH="$JAVA_HOME/bin:$PATH"

HADOOP_BIN="$HADOOP_HOME/bin/hadoop"
HDFS_BIN="$HADOOP_HOME/bin/hdfs"
SUMMARY_DIR="$PROJECT_ROOT/hadoop_summary"
SUMMARY_FILE="$SUMMARY_DIR/run_summary.md"

# 1. Clean up HDFS
echo "Cleaning up previous HDFS directories..."
$HDFS_BIN dfs -rm -r -skipTrash "$HDFS_INPUT_DIR" 2>/dev/null
$HDFS_BIN dfs -rm -r -skipTrash "$HDFS_OUTPUT_DIR" 2>/dev/null
$HDFS_BIN dfs -rm -r -skipTrash "$HDFS_TOPK_OUTPUT_DIR" 2>/dev/null

# 2. Make input directory and upload dataset
echo "Uploading dataset to HDFS..."
$HDFS_BIN dfs -mkdir -p "$HDFS_INPUT_DIR"
$HDFS_BIN dfs -put "$INPUT_CSV" "$HDFS_INPUT_DIR/"

# 3. Run the MapReduce Job using our compiled JAR
echo "Starting Hadoop MapReduce Job (Java)..."

$HADOOP_BIN jar $JAR_PATH CryptoDriver \
    "$HDFS_INPUT_DIR/$(basename "$INPUT_CSV")" \
    "$HDFS_OUTPUT_DIR"

if [ $? -eq 0 ]; then
    echo "MapReduce Job Completed Successfully!"

    echo "Running Top-10 volatility analysis by year..."
    $HADOOP_BIN jar $JAR_PATH TopKVolatilityDriver \
        "$HDFS_OUTPUT_DIR/part-r-00000" \
        "$HDFS_TOPK_OUTPUT_DIR" \
        "$TOPK_K"

    if [ $? -eq 0 ]; then
        echo "Top-10 volatility job completed successfully!"
    else
        echo "Top-10 volatility job failed."
    fi

    # 4. Retrieve output
    echo "Retrieving output from HDFS..."
    rm -rf "$LOCAL_OUTPUT_DIR"
    mkdir -p "$LOCAL_OUTPUT_DIR"
    $HDFS_BIN dfs -get "$HDFS_OUTPUT_DIR/*" "$LOCAL_OUTPUT_DIR/"

    if $HDFS_BIN dfs -test -e "$HDFS_TOPK_OUTPUT_DIR"; then
        rm -rf "$LOCAL_TOPK_OUTPUT_DIR"
        mkdir -p "$LOCAL_TOPK_OUTPUT_DIR"
        $HDFS_BIN dfs -get "$HDFS_TOPK_OUTPUT_DIR/*" "$LOCAL_TOPK_OUTPUT_DIR/"
    fi

    AGG_FILE="$LOCAL_OUTPUT_DIR/part-r-00000"
    TOPK_FILE="$LOCAL_TOPK_OUTPUT_DIR/part-r-00000"

    if [ -f "$AGG_FILE" ] && [ -f "$TOPK_FILE" ]; then
        echo "Generating summary file..."
        mkdir -p "$SUMMARY_DIR"

        GENERATED_AT=$(date '+%Y-%m-%d %H:%M:%S')
        AGG_ROWS=$(wc -l < "$AGG_FILE" | tr -d ' ')
        TOPK_ROWS=$(wc -l < "$TOPK_FILE" | tr -d ' ')
        YEAR_COUNT=$(cut -f1 "$TOPK_FILE" | sort -u | wc -l | tr -d ' ')
        TOP_VOL_LINE=$(awk -F'\t' 'BEGIN{max=-1} {if(($6+0)>max){max=$6;line=$0}} END{print line}' "$AGG_FILE")

        TOP_SYMBOL=$(echo "$TOP_VOL_LINE" | awk -F'\t' '{print $1}')
        TOP_YEAR=$(echo "$TOP_VOL_LINE" | awk -F'\t' '{print $2}')
        TOP_RANGE=$(echo "$TOP_VOL_LINE" | awk -F'\t' '{print $6}')

        {
            echo "# Hadoop Run Summary"
            echo ""
            echo "## Run Metrics"
            echo "| Metric | Value |"
            echo "|---|---|"
            echo "| Generated At | $GENERATED_AT |"
            echo "| Aggregate Rows | $AGG_ROWS |"
            echo "| Top-K Rows | $TOPK_ROWS |"
            echo "| Years Covered | $YEAR_COUNT |"
            echo "| Highest Avg Daily Range (Overall) | $TOP_SYMBOL ($TOP_YEAR) = $TOP_RANGE |"
            echo ""
            echo "## Yearly #1 Most Volatile (Aligned Table)"
            echo '```text'
            printf '%-6s | %-12s | %-15s | %-12s | %-12s | %-12s | %-12s\n' "Year" "Symbol" "AvgDailyRange" "AvgClose" "MaxHigh" "MinLow" "TradingDays"
            printf '%-6s-+-%-12s-+-%-15s-+-%-12s-+-%-12s-+-%-12s-+-%-12s\n' "------" "------------" "---------------" "------------" "------------" "------------" "------------"
            awk -F'\t' '$2==1 {printf("%-6s | %-12s | %-15s | %-12s | %-12s | %-12s | %-12s\n", $1, $3, $4, $5, $6, $7, $8)}' "$TOPK_FILE"
            echo '```'
        } > "$SUMMARY_FILE"

        echo "Summary written to $SUMMARY_FILE"
    else
        echo "Skipping summary generation: required output files not found."
    fi

    echo "Done! Output saved to cloud_assignment/hadoop_output/ and cloud_assignment/hadoop_output_topk/"
else
    echo "MapReduce Job Failed."
fi
