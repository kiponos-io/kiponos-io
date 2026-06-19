#!/usr/bin/env bash
# Publish dev.to articles from a queue file with random 2-3h gaps. Skips already-published.
set -euo pipefail

POST_SCRIPT="${POST_SCRIPT:-$HOME/.grok/skills/devto/scripts/post.py}"
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
    return 0
  fi
  log "Publishing: $md"
  if out=$(python3 "$POST_SCRIPT" "$md" --publish 2>&1); then
    log "OK: $out"
    record_published "$md" "$out" | tee -a "$LOG_FILE"
    return 0
  else
    log "FAILED: $out"
    return 1
  fi
}

mapfile -t ARTICLES < <(grep -v '^\s*#' "$QUEUE_FILE" | grep -v '^\s*$' || true)
log "=== Queue publish started (${#ARTICLES[@]} entries, gap ${MIN_GAP_SEC}-${MAX_GAP_SEC}s) ==="

published_count=0
for i in "${!ARTICLES[@]}"; do
  md="${ARTICLES[$i]}"
  if publish_one "$md"; then
    if [[ "$(already_published "$md")" == "yes" ]]; then
      ((published_count++)) || true
    fi
  else
    log "WARN: failed $md — continuing after gap"
  fi
  if (( i < ${#ARTICLES[@]} - 1 )); then
    gap=$(random_gap)
    log "Waiting ${gap}s (~$(( gap / 3600 ))h $(( (gap % 3600) / 60 ))m) before next..."
    sleep "$gap"
  fi
done

log "=== Queue publish complete ==="