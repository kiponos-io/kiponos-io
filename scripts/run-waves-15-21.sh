#!/usr/bin/env bash
# Publish entire Waves 15-21 queue: pre-check, dev.to, Crunchbase, WhatsApp.
set -euo pipefail

ROOT="/home/moshe/work/kiponos-io"
DOCS="$ROOT/docs"
QUEUE="$DOCS/waves-15-21-master-queue.txt"
SCRIPT_DIR="$ROOT/scripts"
LOG="${LOG:-$HOME/.config/devto/waves-15-21.log}"
MANIFEST="${MANIFEST:-$HOME/.config/devto/published-manifest.json}"
INTERVAL_SEC="${PUBLISH_INTERVAL_SEC:-8100}"
export BRAVE_USER_DATA_DIR="${BRAVE_USER_DATA_DIR:-$HOME/.config/crunchbase/brave-cdp-profile}"

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG"; }

already_published() {
  local md="$1"
  python3 - "$md" "$MANIFEST" <<'PY'
import json, sys
from pathlib import Path
md, mf = sys.argv[1], Path(sys.argv[2])
if not mf.exists():
    sys.exit(1)
data = json.loads(mf.read_text())
# manifest keys are full paths or basenames
for k, v in data.items():
    if Path(k).name == md and v.get("id"):
        sys.exit(0)
sys.exit(1)
PY
}

publish_ready() {
  local md="$1"
  local path="$DOCS/$md"
  [[ -f "$path" ]] || return 1
  bash "$SCRIPT_DIR/pre-publish-check.sh" "$path" >/dev/null 2>&1 || return 1
  return 0
}

log "=== Waves 15-21 publish run (interval ${INTERVAL_SEC}s) ==="
bash "$SCRIPT_DIR/setup-waves-15-21-covers.sh" 2>&1 | tee -a "$LOG" || true

# Wait for background diagram/cover prep if running
PREP_LOG="$HOME/.config/devto/waves-prep.log"
if [[ -f "$PREP_LOG" ]] && ! grep -q "prep complete" "$PREP_LOG" 2>/dev/null; then
  log "Waiting for waves-prep.log to complete..."
  for _ in $(seq 1 360); do
    grep -q "prep complete" "$PREP_LOG" 2>/dev/null && break
    sleep 30
  done
fi

while IFS= read -r line; do
  [[ -z "$line" || "$line" =~ ^# ]] && continue
  md="${line%%#*}"
  md="${md// /}"

  if already_published "$md"; then
    log "SKIP (already published): $md"
    continue
  fi

  if ! publish_ready "$md"; then
    log "SKIP (missing or failed pre-check): $md"
    continue
  fi

  # Ensure cover URL before publish
  python3 "$HOME/.grok/skills/devto/scripts/cover.py" "$DOCS/$md" --ensure --update-md >>"$LOG" 2>&1 || {
    log "SKIP (cover ensure failed): $md"
    continue
  }

  log "Publishing: $md"
  if out=$("$SCRIPT_DIR/publish-aha-article.sh" "$DOCS/$md" 2>&1 | tee -a "$LOG"); then
    log "OK: $md"
    aid=$(echo "$out" | sed -n 's/.*dev.to id \([0-9]*\).*/\1/p' | tail -1)
    if [[ -n "$aid" ]]; then
      python3 - "$DOCS/$md" "$aid" "$MANIFEST" <<'PY'
import json, sys, urllib.request, os
from pathlib import Path
md, aid, mf = sys.argv[1], sys.argv[2], Path(sys.argv[3])
key = load = {}
if mf.exists():
    load = json.loads(mf.read_text())
key_path = str(Path(md).resolve())
key = md if md in load else key_path
api_key = Path.home().joinpath(".config/devto/api_key").read_text().strip()
req = urllib.request.Request(
    f"https://dev.to/api/articles/{aid}",
    headers={"api-key": api_key, "User-Agent": "waves-runner/1.0"},
)
data = json.loads(urllib.request.urlopen(req, timeout=30).read())
load[key_path] = {"id": data["id"], "url": data["url"], "title": data["title"]}
mf.parent.mkdir(parents=True, exist_ok=True)
mf.write_text(json.dumps(load, indent=2) + "\n")
PY
    fi
    cd "$ROOT" && git add "docs/$md" docs/devto-cover*.jpg 2>/dev/null || true
    git diff --cached --quiet || git commit -m "Publish $md" || true
    git push origin master 2>&1 | tee -a "$LOG" || log "WARN: git push failed for $md"
  else
    log "FAIL: $md"
  fi

  log "Sleep ${INTERVAL_SEC}s before next..."
  sleep "$INTERVAL_SEC"
done < "$QUEUE"

log "=== Waves 15-21 complete ==="