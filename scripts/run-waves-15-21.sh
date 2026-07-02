#!/usr/bin/env bash
# Write missing articles (agent/manual), publish entire Waves 15-21 queue.
set -euo pipefail

ROOT="/home/moshe/work/kiponos-io"
DOCS="$ROOT/docs"
QUEUE="$DOCS/waves-15-21-master-queue.txt"
SCRIPT_DIR="$ROOT/scripts"
LOG="${LOG:-$HOME/.config/devto/waves-15-21.log}"
INTERVAL_SEC="${PUBLISH_INTERVAL_SEC:-8100}"

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG"; }

publish_ready() {
  local md="$1"
  local path="$DOCS/$md"
  [[ -f "$path" ]] || return 1
  bash "$SCRIPT_DIR/pre-publish-check.sh" "$path" >/dev/null 2>&1 || return 1
  return 0
}

log "=== Waves 15-21 publish run (interval ${INTERVAL_SEC}s) ==="

while IFS= read -r line; do
  [[ -z "$line" || "$line" =~ ^# ]] && continue
  md="${line%%#*}"
  md="${md// /}"

  if ! publish_ready "$md"; then
    log "SKIP (missing or failed pre-check): $md"
    continue
  fi

  log "Publishing: $md"
  if "$SCRIPT_DIR/publish-aha-article.sh" "$DOCS/$md" 2>&1 | tee -a "$LOG"; then
    log "OK: $md"
    cd "$ROOT" && git add "docs/$md" docs/devto-cover*.jpg 2>/dev/null || true
    git add "docs/$md" 2>/dev/null || true
    git diff --cached --quiet || git commit -m "Add/publish $md" || true
    git push origin master 2>&1 | tee -a "$LOG" || log "WARN: git push failed for $md"
  else
    log "FAIL: $md"
  fi

  log "Sleep ${INTERVAL_SEC}s before next..."
  sleep "$INTERVAL_SEC"
done < "$QUEUE"

log "=== Waves 15-21 complete ==="