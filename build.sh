
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

if [ -d "$JAVA11_HOME" ]; then
    export JAVA_HOME="$JAVA11_HOME"
else
    export JAVA_HOME=$(readlink -f /usr/bin/java | sed 's:bin/java::')
fi
export PATH="$JAVA_HOME/bin:$PATH"

SRC_DIR="$PROJECT_ROOT/src"
BUILD_DIR="$PROJECT_ROOT/build"
JAR_FILE="$PROJECT_ROOT/crypto-analysis.jar"

# Build classpath from Hadoop JARs
HADOOP_CP=$(find "$HADOOP_HOME/share/hadoop" -name '*.jar' | tr '\n' ':')

echo "Compiling Java sources..."
mkdir -p "$BUILD_DIR"
javac -classpath "$HADOOP_CP" -d "$BUILD_DIR" \
    "$SRC_DIR/CryptoMapper.java" \
    "$SRC_DIR/CryptoReducer.java" \
    "$SRC_DIR/CryptoCombiner.java" \
    "$SRC_DIR/CryptoDriver.java" \
    "$SRC_DIR/TopKVolatilityMapper.java" \
    "$SRC_DIR/TopKVolatilityReducer.java" \
    "$SRC_DIR/TopKVolatilityDriver.java"

if [ $? -ne 0 ]; then
    echo "Compilation FAILED!"
    exit 1
fi

echo "Creating JAR..."
jar -cvf "$JAR_FILE" -C "$BUILD_DIR" .

echo "BUILD SUCCESS: $JAR_FILE"
