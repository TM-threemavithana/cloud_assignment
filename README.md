# Cryptocurrency Market Analysis using MapReduce

**Cloud Computing Assignment - Semester 7**  
**Module**: Cloud Computing (EE7222/EC7204)  


## Project Overview

This project implements a comprehensive MapReduce solution to analyze large-scale historical cryptocurrency trading data using Apache Hadoop. The analysis extracts meaningful financial insights including yearly price trends, trading volumes, and volatility (average daily range) for the top 100 cryptocurrencies spanning from 2018 to 2025.

## Dataset Description

- **Source**: Historical Cryptocurrency Trading Data (Kaggle)
- **Size**: 211,000+ daily trading records
- **Format**: CSV
- **Columns**: Symbol, Date, Open, High, Low, Close, Network

**Recommended Kaggle Datasets** (if simulating independently):
1. Top 100 Cryptocurrencies Historical Data

## MapReduce Task

**Objective**: Multi-dimensional cryptocurrency price aggregation, volatility analysis, and Top-K ranking across multiple years.

**Primary Job (Yearly Aggregation)**
- **Mapper Output**: Key: `Symbol, Year` | Value: `High, Low, Close`
- **Combiner Output**: Partial local aggregation to drastically minimize network shuffle.
- **Reducer Output**: 
  - Average Close Price
  - Maximum High
  - Minimum Low
  - Average Daily Range (Volatility)
  - Trading Days

**Secondary Job (Top-K Volatility Ranking)**
- **Mapper Output**: Groups by `Year`
- **Reducer Output**: Ranks the top 10 most volatile cryptocurrencies per year based on their Average Daily Range.

## Prerequisites

- Ubuntu 20.04 or later (or WSL2 on Windows)
- **Java**: OpenJDK 11 (`default-jdk`)
- **Hadoop**: Apache Hadoop 3.3.6
- 4GB+ RAM recommended
- Internet connection for initial setup

## Installation & Setup

### Step 1: Clone/Download the Project

```bash
git clone <your-repo-url>
cd cloud_assignment
```

### Step 2: Install Hadoop & Java

On a fresh WSL/Ubuntu instance, install dependencies:

```bash
sudo apt update
sudo apt install default-jdk ssh pdsh unzip dos2unix
```

**Download and Extract Hadoop 3.3.6:**
```bash
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
sudo tar -xzvf hadoop-3.3.6.tar.gz -C /usr/local/
sudo mv /usr/local/hadoop-3.3.6 /usr/local/hadoop
```

*(Edit the local `.env` file to ensure `PROJECT_ROOT`, `JAVA11_HOME`, and `HADOOP_HOME` point to your correct system directories.)*

### Step 3: Start Hadoop Services

```bash
# Ensure Hadoop is formatted
hdfs namenode -format

# Start HDFS
$HADOOP_HOME/sbin/start-dfs.sh

# Start YARN
$HADOOP_HOME/sbin/start-yarn.sh

# Verify services are running
jps
```

You should see processes like `NameNode`, `DataNode`, `ResourceManager`, `NodeManager`, and `SecondaryNameNode`.

**Web Interfaces**:
- HDFS NameNode: http://localhost:9870
- YARN ResourceManager: http://localhost:8088

## Running the MapReduce Job

### Execute the Complete Pipeline

This project contains an automated orchestration script that builds the Java code, uploads data to Hadoop, and executes the dual MapReduce jobs.

```bash
cd /path/to/project
dos2unix .env build.sh run_hadoop.sh start_and_run_hadoop.sh && chmod +x .env build.sh run_hadoop.sh start_and_run_hadoop.sh
./build.sh
./start_and_run_hadoop.sh
```

This script will:
1. Start the Hadoop cluster
2. Compile the Java MapReduce sources into `crypto-analysis.jar` (via `build.sh`)
3. Clean old HDFS directories and upload the target dataset
4. Run the Primary MapReduce job
5. Run the Top-K Volatility MapReduce job
6. Download results to `hadoop_output/` and `hadoop_output_topk/`
7. Generate an aligned summary Markdown file in `hadoop_summary/`
8. Safely shut down all Hadoop daemons

## Output Format

Results are cleanly tab-separated with the following column structure:

**Primary Aggregation (`hadoop_output/`)**:
```
<Symbol>\t<Year>\t<AvgClose>\t<MaxHigh>\t<MinLow>\t<AvgDailyRange>\t<TradingDays>
```

**Example**:
```text
BTCUSDT    2021    47400.0030    69000.0000    28130.0000    3132.4475    365
ETHUSDT    2021    2777.4287     4868.0000     714.2900      224.9295     365
```

## Project Structure

```text
cloud_assignment/
├── src/
│   ├── CryptoMapper.java        # Primary Mapper
│   ├── CryptoCombiner.java      # Optimization Combiner
│   ├── CryptoReducer.java       # Primary Reducer
│   ├── CryptoDriver.java        # Primary Config/Driver
│   ├── TopKVolatilityMapper.java   # Top-K Mapper
│   ├── TopKVolatilityReducer.java  # Top-K Reducer
│   └── TopKVolatilityDriver.java   # Top-K Config/Driver
├── data/
│   └── top_100_cryptos_*.csv    # Input dataset
├── build.sh                     # Java JAR compilation script
├── run_hadoop.sh                # MapReduce job execution script
├── start_and_run_hadoop.sh      # Daemon management wrapper
├── .env                         
├── hadoop_output/               # Primary aggregated MapReduce results
├── hadoop_output_topk/          # Top-10 volatility ranking results
├── hadoop_summary/              # Auto-generated run summary markdowns
└── README.md                    
```



## Troubleshooting

### Hadoop JVM Memory Limits (Out of Memory Kills)
If the WSL operating system violently kills the MapReduce execution process during the Java execution, it means WSL ran out of memory. This project already strictly limits JVM heaps explicitly in `run_hadoop.sh` via:
```bash
export HADOOP_CLIENT_OPTS="-Xmx512m"
export YARN_CLIENT_OPTS="-Xmx512m"
```

### Windows Line Endings (CRLF) in Scripts
If bash throws `command not found` or `$'\r'` errors, run `dos2unix` on your scripts:
```bash
dos2unix build.sh run_hadoop.sh start_and_run_hadoop.sh .env
```

## Performance Observations

- **Dataset Size**: ~211,680 records
- **Processing Time**: ~1 to 2 minutes on WSL2 (hardware dependent)
- **Optimizations**: Adding the robust `CryptoCombiner` radically minimizes cross-node network traffic by pre-aggregating Map data perfectly prior to the shuffle phase.

## Future Enhancements

1. Add real-time streaming analysis utilizing Apache Kafka.
2. Integration with Apache Spark to compare MapReduce vs in-memory performance.
3. Expanded complex pattern recognition logic for predicting bullish cycles.

## References

- [Apache Hadoop Documentation](https://hadoop.apache.org/docs/stable/)
- [Hadoop MapReduce Tutorial](https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html)

## Team Members

Karunarathne S.M.G.S. – EG/2021/4602  
Senevirathne P.U.S. - EG/2021/4805  
Threemavithana T.M. - EG/2021/4835  

---
**University of Ruhuna - Faculty of Engineering**
