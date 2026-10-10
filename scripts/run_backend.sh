#!/usr/bin/env bash
set -e

echo "=========================================="
echo "Starting AutoScientist AI Backend"
echo "=========================================="

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR/backend"

if [ -f ".env" ]; then
    echo "Loading environment variables from backend/.env..."
    set -a
    source .env
    set +a
fi

if [ ! -f "target/classpath.txt" ]; then
    echo "Generating classpath..."
    ./mvnw dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -q
fi

exec java -cp "target/classes:$(cat target/classpath.txt)" com.autoscientist.AutoScientistApplication
