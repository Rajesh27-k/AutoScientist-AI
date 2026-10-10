#!/usr/bin/env bash
set -e

echo "=========================================="
echo "Starting AutoScientist AI Frontend"
echo "=========================================="

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR/frontend"

exec npm run dev -- --host
