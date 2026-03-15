# Large-Scale Data Analysis Using MapReduce
### Cryptocurrency Market Analysis - Historical Trading Data

**Cloud Computing Assignment - Semester 7**  
**Module**: Cloud Computing (EE7222/EC7204)

## Overview

This project implements a two-stage Hadoop MapReduce pipeline to analyze large-scale historical cryptocurrency trading data. Using **Java MapReduce** and **Apache Hadoop**, the analysis aggregates yearly price metrics, computes volatility (average daily range), and ranks the Top-K most volatile cryptocurrencies per year across the Top 100 coins from 2018 to 2025.

| Component | Detail |
|---|---|
| Dataset | Historical Cryptocurrency Trading Data - 211,680 rows |
| Task | Yearly price aggregation and Top-K volatility ranking |
| MapReduce Key | Composite keys: `Symbol,Year` (aggregation) and `Year` (Top-K) |
| Language | Java 11 |
| Platform | Hadoop 3.3.6 - Pseudo-Distributed (WSL2 / Ubuntu) |

---

## Dataset

- **Source:** Historical Cryptocurrency Trading Data - Kaggle
- **Size:** 211,680 daily trading records collected between 2018-2025
- **Key columns used:** `Symbol` (asset), `Date` (time dimension), `High`, `Low`, and `Close` (trading metrics)
- **Local file used:** `data/top_100_cryptos_with_correct_network.csv`
- **Note:** If the dataset is missing, download it and place it in `data/`.

## Project Structure

```text
cloud_assignment/
|-- src/
|   |-- CryptoMapper.java              # Primary mapper
|   |-- CryptoCombiner.java            # Combiner (local aggregation)
|   |-- CryptoReducer.java             # Primary reducer
|   |-- CryptoDriver.java              # Primary job driver
|   |-- TopKVolatilityMapper.java      # Top-K mapper
|   |-- TopKVolatilityReducer.java     # Top-K reducer
|   |-- TopKVolatilityDriver.java      # Top-K job driver
|-- data/
|   |-- top_100_cryptos_with_correct_network.csv
|-- build.sh                           # Java JAR compilation script
|-- run_hadoop.sh                      # MapReduce execution script
|-- start_and_run_hadoop.sh            # Daemon management wrapper
|-- .env                               # Environment configuration
|-- hadoop_output/                     # Primary aggregation results
|-- hadoop_output_topk/                # Top-K volatility results
|-- hadoop_summary/                    # Auto-generated run summary
|-- README.md
```

---

## How It Works

```text
INPUT (CSV: Symbol, Date, High, Low, Close)
       |
       v
PRIMARY MAPREDUCE JOB
- Mapper: emits Symbol,Year -> High,Low,Close
- Combiner: local partial aggregation
- Reducer: averages and min/max metrics per Symbol,Year
       |
       v
OUTPUT (Aggregation)
- Symbol,Year,AvgClose,MaxHigh,MinLow,AvgDailyRange,TradingDays
       |
       v
SECONDARY MAPREDUCE JOB (Top-K)
- Mapper: groups by Year
- Reducer: ranks Top-K most volatile symbols per year
       |
       v
OUTPUT (Top-K)
- Year,Rank,Symbol,AvgDailyRange,AvgClose,MaxHigh,MinLow,TradingDays
```

---

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

---

## Prerequisites

- Ubuntu 20.04 or later (or WSL2 on Windows)
- **Java**: OpenJDK 11 (`default-jdk`)
- **Hadoop**: Apache Hadoop 3.3.6
- 4GB+ RAM recommended

### Verify Your Environment

```bash
java -version       # Should show JDK 11
hadoop version      # Should show Hadoop 3.3.6
jps                 # Should show 5 daemons running
```

---

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

Edit `.env` to ensure `PROJECT_ROOT`, `JAVA11_HOME`, and `HADOOP_HOME` point to your correct system directories.

---

## Execution Guide

### Step 1 - Start Hadoop Daemons

```bash
hdfs namenode -format
$HADOOP_HOME/sbin/start-dfs.sh
$HADOOP_HOME/sbin/start-yarn.sh
jps
```

You should see:
- NameNode
- DataNode
- ResourceManager
- NodeManager
- SecondaryNameNode

**Web Interfaces**:
- HDFS NameNode: http://localhost:9870
- YARN ResourceManager: http://localhost:8088

### Step 2 - Upload Dataset to HDFS

```bash
hdfs dfs -mkdir -p /crypto_input
hdfs dfs -put data/top_100_cryptos_with_correct_network.csv /crypto_input/
hdfs dfs -ls /crypto_input
```

### Step 3 - Run the MapReduce Pipeline (Scripted)

```bash
dos2unix .env build.sh run_hadoop.sh start_and_run_hadoop.sh
chmod +x .env build.sh run_hadoop.sh start_and_run_hadoop.sh
./build.sh
./start_and_run_hadoop.sh
```

This script will:
1. Start the Hadoop cluster
2. Compile the Java MapReduce sources into `crypto-analysis.jar`
3. Clean old HDFS directories and upload the dataset
4. Run the primary aggregation MapReduce job
5. Run the Top-K volatility MapReduce job
6. Download results to `hadoop_output/` and `hadoop_output_topk/`
7. Generate a run summary in `hadoop_summary/`
8. Safely shut down all Hadoop daemons

### Step 4 - Run the MapReduce Pipeline (Manual)

```bash
hadoop jar crypto-analysis.jar CryptoDriver \
  /crypto_input/top_100_cryptos_with_correct_network.csv \
  /crypto_output

hadoop jar crypto-analysis.jar TopKVolatilityDriver \
  /crypto_output/part-r-00000 \
  /crypto_topk_output \
  10
```

### Step 5 - Retrieve Results

```bash
hdfs dfs -get /crypto_output/part-r-00000 hadoop_output/
hdfs dfs -get /crypto_topk_output/part-r-00000 hadoop_output_topk/
```

---

## Output Format

Results are tab-separated with the following column structure:

**Primary Aggregation (`hadoop_output/`)**:
```
<Symbol>\t<Year>\t<AvgClose>\t<MaxHigh>\t<MinLow>\t<AvgDailyRange>\t<TradingDays>
```

**Example**:
```text
BTCUSDT    2021    47400.0030    69000.0000    28130.0000    3132.4475    365
ETHUSDT    2021    2777.4287     4868.0000     714.2900      224.9295     365
```

**Top-K Volatility (`hadoop_output_topk/`)**:
```
<Year>\t<Rank>\t<Symbol>\t<AvgDailyRange>\t<AvgClose>\t<MaxHigh>\t<MinLow>\t<TradingDays>
```

---

## Results & Insights

- The Top-K job highlights the most volatile assets per year, which helps surface high-risk, high-movement periods.
- Example from 2018 Top-K output: `BTCUSDT` ranks #1, followed by `ETHUSDT` and `LTCUSDT`.
- The aggregated output makes it easy to compare average price levels and volatility across years for each symbol.

---

## Troubleshooting

### Hadoop JVM Memory Limits (Out of Memory Kills)
If the WSL operating system kills the MapReduce execution process, it means WSL ran out of memory. This project limits JVM heaps in `run_hadoop.sh`:

```bash
export HADOOP_CLIENT_OPTS="-Xmx512m"
export YARN_CLIENT_OPTS="-Xmx512m"
```

### Windows Line Endings (CRLF) in Scripts
If bash throws `command not found` or `$'\r'` errors, run `dos2unix`:

```bash
dos2unix build.sh run_hadoop.sh start_and_run_hadoop.sh .env
```

---

## Performance Observations

- **Dataset Size**: ~211,680 records
- **Processing Time**: ~1 to 2 minutes on WSL2 (hardware dependent)
- **Optimizations**: The `CryptoCombiner` reduces shuffle traffic by pre-aggregating map output locally.

---

## Future Enhancements

1. Add real-time streaming analysis using Apache Kafka.
2. Integrate Apache Spark to compare MapReduce vs in-memory performance.
3. Expand predictive analytics for identifying volatility spikes.

---

## References

- Apache Hadoop Documentation: https://hadoop.apache.org/docs/stable/
- Hadoop MapReduce Tutorial: https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html

---

## Team Members

Karunarathne S.M.G.S. - EG/2021/4602  
Senevirathne P.U.S. - EG/2021/4805  
Threemavithana T.M. - EG/2021/4835

---
**University of Ruhuna - Faculty of Engineering**
