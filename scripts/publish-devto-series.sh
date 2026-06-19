#!/usr/bin/env bash
# Publish a queue of dev.to articles with random 2-3 hour gaps between each.
set -euo pipefail

POST_SCRIPT="${POST_SCRIPT:-$HOME/.grok/skills/devto/scripts/post.py}"
LOG_DIR="${LOG_DIR:-$HOME/.config/devto}"
LOG_FILE="$LOG_DIR/series-publish.log"
MANIFEST="$LOG_DIR/series-manifest.json"
MIN_GAP_SEC="${MIN_GAP_SEC:-7200}"   # 2 hours
MAX_GAP_SEC="${MAX_GAP_SEC:-10800}"  # 3 hours

mkdir -p "$LOG_DIR"

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG_FILE"; }

random_gap() {
  echo $(( MIN_GAP_SEC + RANDOM % (MAX_GAP_SEC - MIN_GAP_SEC + 1) ))
}

publish_one() {
  local md="$1"
  log "Publishing: $md"
  if out=$(python3 "$POST_SCRIPT" "$md" --publish 2>&1); then
    log "OK: $out"
    echo "$out" >> "$LOG_FILE"
    return 0
  else
    log "FAILED: $out"
    echo "$out" >> "$LOG_FILE"
    return 1
  fi
}

# Queue: articles #4-#10 (already published #1-#3)
ARTICLES=(
  "/home/moshe/work/kiponos-io/docs/devto-rate-limits-circuit-breakers.md"
  "/home/moshe/work/kiponos-io/docs/devto-ab-checkout-weights.md"
  "/home/moshe/work/kiponos-io/docs/devto-trading-bot-risk.md"
  "/home/moshe/work/kiponos-io/docs/devto-game-server-balance.md"
  "/home/moshe/work/kiponos-io/docs/devto-iot-sensor-calibration.md"
  "/home/moshe/work/kiponos-io/docs/devto-hospital-triage-routing.md"
  "/home/moshe/work/kiponos-io/docs/devto-llm-inference-serving.md"
)

log "=== Series publish started (${#ARTICLES[@]} articles, gap ${MIN_GAP_SEC}-${MAX_GAP_SEC}s) ==="

for i in "${!ARTICLES[@]}"; do
  publish_one "${ARTICLES[$i]}" || log "WARN: article $((i+4)) failed, continuing after gap"
  if (( i < ${#ARTICLES[@]} - 1 )); then
    gap=$(random_gap)
    log "Waiting ${gap}s (~$(( gap / 3600 ))h $(( (gap % 3600) / 60 ))m) before next article..."
    sleep "$gap"
  fi
done

log "=== Series publish complete ==="