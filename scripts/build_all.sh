#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=========================================="
echo "1. Building Backend..."
echo "=========================================="
cd "$DIR/backend"
./mvnw clean package -DskipTests

echo "=========================================="
echo "2. Building Frontend..."
echo "=========================================="
cd "$DIR/frontend"
npm run build

echo "=========================================="
echo "Build Completed Successfully!"
echo "=========================================="
