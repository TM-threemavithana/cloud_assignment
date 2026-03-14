#!/bin/bash
# build.sh — Compile Java MapReduce classes and create JAR

source ~/.bashrc

SRC_DIR="/mnt/c/Users/User/cloud_assignment/src"
BUILD_DIR="/mnt/c/Users/User/cloud_assignment/build"
JAR_FILE="/mnt/c/Users/User/cloud_assignment/crypto-analysis.jar"

# Build classpath from Hadoop JARs
HADOOP_CP=$(find /usr/local/hadoop/share/hadoop -name '*.jar' | tr '\n' ':')

echo "Compiling Java sources..."
mkdir -p "$BUILD_DIR"
javac -classpath "$HADOOP_CP" -d "$BUILD_DIR" \
    "$SRC_DIR/CryptoMapper.java" \
    "$SRC_DIR/CryptoReducer.java" \
    "$SRC_DIR/CryptoCombiner.java" \
    "$SRC_DIR/CryptoDriver.java"

if [ $? -ne 0 ]; then
    echo "Compilation FAILED!"
    exit 1
fi

echo "Creating JAR..."
jar -cvf "$JAR_FILE" -C "$BUILD_DIR" .

echo "BUILD SUCCESS: $JAR_FILE"
