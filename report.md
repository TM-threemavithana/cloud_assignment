# Cryptocurrency Trading Volumes and Price Trends
## MapReduce Analysis Report

### 1. Introduction
This report details the analysis of a historical cryptocurrency dataset containing daily trading data for the top 100 cryptocurrencies between August 2018 and October 2025. The primary objective is to evaluate long-term trends, market cycles (bull and bear phases), and volatility across major digital assets using a distributed computing framework (Hadoop MapReduce).

### 2. Methodology
A custom **Java-based** MapReduce pipeline was designed and executed in a pseudo-distributed Apache Hadoop 3.3.6 cluster running on Windows Subsystem for Linux (WSL - Ubuntu).

*   **Mapper (`CryptoMapper.java`)**: Extends `Mapper<LongWritable, Text, Text, Text>`. Parses each CSV line, extracting the `Symbol`, `Year` (derived from the date), and the daily `High`, `Low`, and `Close` prices. Emits a composite key `Symbol,Year` mapped to values `High,Low,Close`. Malformed rows are silently skipped.
*   **Reducer (`CryptoReducer.java`)**: Extends `Reducer<Text, Text, Text, Text>`. Iterates over all values for a given `Symbol,Year` key and calculates:
    *   **Average Close Price**: The mean closing price for the year.
    *   **Max High**: The absolute highest price reached during that year.
    *   **Min Low**: The absolute lowest price recorded.
    *   **Average Daily Volatility**: The mean difference between each day's High and Low.
    *   **Total Trading Days**: A validation metric indicating data completeness.
*   **Driver (`CryptoDriver.java`)**: Configures the Hadoop `Job` object, binds the Mapper and Reducer classes, and submits the job to YARN.

### 3. Key Findings & Market Trends

#### 3.1 The 2021 Macro Bull Cycle
The MapReduce output clearly highlights 2021 as a defining year of exponential growth across the entire cryptocurrency sector.
*   **Bitcoin (BTC) & Ethereum (ETH)**: BTC's average close price surged from $11,110 in 2020 to $47,400 in 2021, peaking at $69,000. ETH accompanied this trend, growing its annual average from $307 in 2020 to $2,777 in 2021, with a maximum high of $4,868.
*   **Altcoin Explosions**: Smaller-cap assets experienced even larger percentage gains. For example, Solana (SOL) jumped from an average of $2.40 (2020) to $80.09 (2021), while Dogecoin (DOGE) rose from a fraction of a cent to average $0.20 for the year, hitting a peak of $0.74.

#### 3.2 The 2022-2023 Bear Market "Winter"
Following the 2021 highs, 2022 marked a steep contraction, often referred to as a "crypto winter."
*   BTC's average price dropped to $28,185 in 2022, and ETH fell to $1,986. Both experienced severe drawdowns (BTC low of $15,476; ETH low of $881).
*   2023 showed prolonged consolidation with BTC averaging $28,849, demonstrating market-wide stabilization rather than immediate recovery.

#### 3.3 The 2024-2025 Resurgence
The dataset reveals a massive structural recovery beginning in 2024 and extending through 2025, pushing assets well beyond their 2021 peaks.
*   **New ATHs**: BTC's average rocketed to $65,963 in 2024, crossing $100K to hit a max of $108,353. In 2025, BTC averaged $99,228 with a peak of $123,218.
*   **Volatility Metrics**: BTC's average daily price swing scaled from ~$548 in 2020 → ~$3,132 in 2021 → ~$957 in 2023 → ~$3,350 in 2025, showing that absolute volatility scales proportionally with nominal prices.

### 4. Technical Conclusion
The Java-based MapReduce paradigm successfully synthesized millions of discrete daily data points into a concise, interpretable timeline of the cryptocurrency market over an 8-year horizon. Using native Java MapReduce (rather than Hadoop Streaming) provides tighter integration with the Hadoop ecosystem, type safety, and better performance for large-scale datasets. Future enhancements could involve computing moving averages within the reducer or incorporating volume-weighting into price calculations.
