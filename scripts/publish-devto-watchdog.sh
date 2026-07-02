#!/usr/bin/env bash
# Keeps publish-devto-queue.sh running across 10h sandbox kills and reboots.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
QUEUE_SCRIPT="$SCRIPT_DIR/publish-devto-queue.sh"
LOG_DIR="${LOG_DIR:-$HOME/.config/devto}"
LOG_FILE="$LOG_DIR/watchdog.log"
MANIFEST="${MANIFEST:-$LOG_DIR/published-manifest.json}"
QUEUE_FILE="${QUEUE_FILE:-$LOG_DIR/publish-queue.txt}"

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG_FILE"; }

queue_remaining() {
  python3 - "$MANIFEST" "$QUEUE_FILE" <<'PY'
import json, sys
from pathlib import Path
manifest_path, queue_path = sys.argv[1], sys.argv[2]
done = set(json.load(open(manifest_path)).keys()) if Path(manifest_path).exists() else set()
queued = [l.strip() for l in Path(queue_path).read_text().splitlines() if l.strip() and not l.startswith('#')]
pending = [q for q in queued if q not in done]
print(len(pending))
PY
}

log "=== Watchdog started ==="

while true; do
  remaining=$(queue_remaining)
  if [[ "$remaining" -eq 0 ]]; then
    log "All articles published. Watchdog exiting."
    exit 0
  fi
  log "Starting queue ($remaining remaining)..."
  if bash "$QUEUE_SCRIPT"; then
    log "Queue script exited cleanly."
  else
    log "Queue script exited with error $?"
  fi
  remaining=$(queue_remaining)
  if [[ "$remaining" -eq 0 ]]; then
    log "All articles published. Watchdog exiting."
    exit 0
  fi
  log "Restarting in 60s ($remaining still pending)..."
  sleep 60
done