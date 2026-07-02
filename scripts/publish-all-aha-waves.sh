#!/usr/bin/env bash
# Publish all articles listed in queue files (diagram + cover + dev.to + CB + WhatsApp).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOCS=/home/moshe/work/kiponos-io/docs
LOG="${LOG:-$HOME/.config/devto/wave-aha-publish.log}"
GAP="${PUBLISH_GAP_SEC:-30}"

mkdir -p "$(dirname "$LOG")"
bash "$SCRIPT_DIR/setup-aha-covers.sh"

publish_queue() {
  local qf="$1"
  echo "=== Queue: $qf ===" | tee -a "$LOG"
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%%#*}"
    line="${line// /}"
    [[ -z "$line" ]] && continue
    md="$DOCS/$line"
    [[ -f "$md" ]] || { echo "MISSING $md" | tee -a "$LOG"; continue; }
    echo "--- $(date -Iseconds) $md ---" | tee -a "$LOG"
    if bash "$SCRIPT_DIR/publish-aha-article.sh" "$md" 2>&1 | tee -a "$LOG"; then
      echo "OK $md" | tee -a "$LOG"
    else
      echo "FAIL $md (continuing)" | tee -a "$LOG"
    fi
    sleep "$GAP"
  done < "$qf"
}

publish_queue "$DOCS/wave12-aha-publish-queue.txt"
publish_queue "$DOCS/wave13-aha-publish-queue.txt"
publish_queue "$DOCS/wave14-aha-publish-queue.txt"

python3 ~/.grok/skills/crunchbase-news/scripts/list_press_gaps.py | tee -a "$LOG"
echo "All waves complete. Log: $LOG"