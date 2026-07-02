#!/usr/bin/env bash
# Publish dev.to articles from a queue file with random 2-3h gaps. Skips already-published.
set -euo pipefail

POST_SCRIPT="${POST_SCRIPT:-$HOME/.grok/skills/devto/scripts/post.py}"
NOTIFY_SCRIPT="${NOTIFY_SCRIPT:-$(cd "$(dirname "$0")" && pwd)/notify-devto-whatsapp.sh}"
LOG_DIR="${LOG_DIR:-$HOME/.config/devto}"
QUEUE_FILE="${QUEUE_FILE:-$LOG_DIR/publish-queue.txt}"
MANIFEST="${MANIFEST:-$LOG_DIR/published-manifest.json}"
LOG_FILE="$LOG_DIR/series-publish.log"
MIN_GAP_SEC="${MIN_GAP_SEC:-7200}"
MAX_GAP_SEC="${MAX_GAP_SEC:-10800}"

mkdir -p "$LOG_DIR"
[[ -f "$MANIFEST" ]] || echo '{}' > "$MANIFEST"

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG_FILE"; }

random_gap() {
  echo $(( MIN_GAP_SEC + RANDOM % (MAX_GAP_SEC - MIN_GAP_SEC + 1) ))
}

already_published() {
  local md="$1"
  python3 - "$md" "$MANIFEST" <<'PY'
import json, sys
md, path = sys.argv[1], sys.argv[2]
try:
    data = json.load(open(path))
except Exception:
    data = {}
print("yes" if md in data else "no")
PY
}

record_published() {
  local md="$1" json_out="$2"
  python3 - "$md" "$json_out" "$MANIFEST" <<'PY'
import json, sys
md, raw, path = sys.argv[1], sys.argv[2], sys.argv[3]
data = json.load(open(path)) if open(path).read().strip() else {}
try:
    info = json.loads(raw)
except Exception:
    info = {"raw": raw}
data[md] = {"id": info.get("id"), "url": info.get("url"), "title": info.get("title")}
json.dump(data, open(path, "w"), indent=2)
print(json.dumps(data[md], indent=2))
PY
}

publish_one() {
  local md="$1"
  if [[ "$(already_published "$md")" == "yes" ]]; then
    log "SKIP (already published): $md"
    echo "skipped"
    return 0
  fi
  log "Publishing: $md"
  if out=$(python3 "$POST_SCRIPT" "$md" --publish 2>&1); then
    log "OK: $out"
    record_published "$md" "$out" | tee -a "$LOG_FILE"
    if [[ -x "$NOTIFY_SCRIPT" || -f "$NOTIFY_SCRIPT" ]]; then
      pub_id=$(echo "$out" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)
      pub_url=$(echo "$out" | python3 -c "import sys,json; print(json.load(sys.stdin).get('url',''))" 2>/dev/null || true)
      pub_title=$(echo "$out" | python3 -c "import sys,json; print(json.load(sys.stdin).get('title',''))" 2>/dev/null || true)
      if bash "$NOTIFY_SCRIPT" "$pub_title" "$pub_url" "$pub_id" "$md" 2>&1 | tee -a "$LOG_FILE"; then
        log "WhatsApp notify OK"
      else
        log "WARN: WhatsApp notify failed (daemon down?)"
      fi
    fi
    echo "published"
    return 0
  else
    log "FAILED: $out"
    echo "failed"
    return 1
  fi
}

mapfile -t ARTICLES < <(grep -v '^\s*#' "$QUEUE_FILE" | grep -v '^\s*$' || true)
log "=== Queue publish started (${#ARTICLES[@]} entries, gap ${MIN_GAP_SEC}-${MAX_GAP_SEC}s) ==="

for i in "${!ARTICLES[@]}"; do
  md="${ARTICLES[$i]}"
  result=$(publish_one "$md" | tail -1)
  if [[ "$result" == "failed" ]]; then
    log "WARN: failed $md — continuing after gap"
  fi
  if [[ "$result" == "published" ]] && (( i < ${#ARTICLES[@]} - 1 )); then
    gap=$(random_gap)
    log "Waiting ${gap}s (~$(( gap / 3600 ))h $(( (gap % 3600) / 60 ))m) before next..."
    sleep "$gap"
  fi
done

log "=== Queue publish complete ==="