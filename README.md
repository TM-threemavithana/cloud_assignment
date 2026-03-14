# Hadoop MapReduce: Cryptocurrency Market Analysis (Java)

This project analyzes the historical trading volumes and price trends of the top 100 cryptocurrencies (2018-2025) using a **Java-based** Apache Hadoop MapReduce pipeline on a pseudo-distributed cluster managed inside Windows Subsystem for Linux (WSL).

## Prerequisites

1.  **Windows Subsystem for Linux (WSL2)**: Installed with an Ubuntu distribution.
2.  **Java**: OpenJDK 11 (`default-jdk`).
3.  **Hadoop**: Apache Hadoop 3.3.6 (installed in `/usr/local/hadoop`).

## Environment Setup Instructions

If you are setting this up for the first time on a fresh WSL instance, follow these steps:

1.  **Install Dependencies**:
    ```bash
    sudo apt update
    sudo apt install default-jdk ssh pdsh unzip dos2unix
    ```
2.  **Download and Extract Hadoop**:
    ```bash
    wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
    sudo tar -xzvf hadoop-3.3.6.tar.gz -C /usr/local/
    sudo mv /usr/local/hadoop-3.3.6 /usr/local/hadoop
    ```
3.  **Configure Environment**:
    Use the provided `setup_env.sh` and `config_hadoop.py` scripts to configure your `.bashrc` and Hadoop's XML configuration files (`core-site.xml`, `hdfs-site.xml`, etc.) for pseudo-distributed mode.
4.  **Format HDFS**:
    ```bash
    hdfs namenode -format
    ```

## Java MapReduce Source Code

The MapReduce logic is implemented in Java under the `src/` directory:

| File | Description |
|------|-------------|
| `src/CryptoMapper.java` | Parses CSV rows, extracts `Symbol,Year` as key and `High,Low,Close` as value |
| `src/CryptoReducer.java` | Aggregates daily data into yearly metrics: Avg Close, Max High, Min Low, Avg Daily Range, Trading Days |
| `src/CryptoDriver.java` | Configures and launches the MapReduce job |

### Building the JAR

```bash
cd /mnt/c/Users/User/cloud_assignment
dos2unix build.sh && chmod +x build.sh
./build.sh
```

This compiles the Java sources against the Hadoop classpath and packages them into `crypto-analysis.jar`.

## Executing the MapReduce Job

Run the entire pipeline (start Hadoop → upload data → run job → retrieve output → stop Hadoop):

```bash
cd /mnt/c/Users/User/cloud_assignment
dos2unix start_and_run_hadoop.sh && chmod +x start_and_run_hadoop.sh
./start_and_run_hadoop.sh
```

### What this script does:
1.  **Starts Hadoop**: Launches HDFS (NameNode/DataNode) and YARN (ResourceManager/NodeManager).
2.  **Waits for SafeMode**: Ensures NameNode is ready.
3.  **Uploads Data & Runs Job**: Calls `run_hadoop.sh` which uploads the CSV to HDFS and executes `hadoop jar crypto-analysis.jar CryptoDriver`.
4.  **Retrieves Output**: Copies results from HDFS to `hadoop_output/part-r-00000`.
5.  **Stops Hadoop**: Safely shuts down all daemons.

## Files in this Repository

| File | Description |
|------|-------------|
| `src/CryptoMapper.java` | Java Mapper class |
| `src/CryptoReducer.java` | Java Reducer class |
| `src/CryptoDriver.java` | Java Driver class |
| `build.sh` | Script to compile Java and create `crypto-analysis.jar` |
| `run_hadoop.sh` | HDFS upload + MapReduce execution script |
| `start_and_run_hadoop.sh` | Wrapper to start/stop Hadoop around the job |
| `data/` | Kaggle dataset directory |
| `hadoop_output/` | Final aggregated MapReduce results |
| `report.md` | 2-page analysis report |
