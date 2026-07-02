#!/usr/bin/env bash
# Publish a queue of articles with organic spacing (default 2h 15m between posts).
set -euo pipefail

QUEUE="${1:-/home/moshe/work/kiponos-io/docs/wave12-aha-publish-queue.txt}"
INTERVAL_SEC="${PUBLISH_INTERVAL_SEC:-8100}"  # 2h 15m
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

while IFS= read -r line; do
  [[ -z "$line" || "$line" =~ ^# ]] && continue
  md="${line%%#*}"
  md="${md// /}"
  path="/home/moshe/work/kiponos-io/docs/$md"
  [[ -f "$path" ]] || { echo "Skip missing: $path"; continue; }
  echo "=== $(date -Iseconds) Publishing $md ==="
  "$SCRIPT_DIR/publish-aha-article.sh" "$path"
  echo "=== Sleeping ${INTERVAL_SEC}s before next ==="
  sleep "$INTERVAL_SEC"
done < "$QUEUE"

echo "Queue complete: $QUEUE"