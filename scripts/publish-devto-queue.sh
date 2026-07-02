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

PUBLISH_SCRIPT="${PUBLISH_SCRIPT:-$(cd "$(dirname "$0")" && pwd)/publish-aha-article.sh}"

publish_one() {
  local md="$1"
  if [[ "$(already_published "$md")" == "yes" ]]; then
    log "SKIP (already published): $md"
    echo "skipped"
    return 0
  fi
  log "Publishing (dev.to + Crunchbase + WhatsApp): $md"
  if out=$("$PUBLISH_SCRIPT" "$md" 2>&1); then
    log "OK: $out"
    pub_json=$(echo "$out" | python3 -c "
import re,sys,json,urllib.request,os
m=re.search(r'dev\.to id (\d+)', sys.stdin.read())
if not m: raise SystemExit(1)
aid=m.group(1)
key=open(os.path.expanduser('~/.config/devto/api_key')).read().strip()
req=urllib.request.Request(f'https://dev.to/api/articles/{aid}', headers={'api-key':key})
data=json.loads(urllib.request.urlopen(req,timeout=30).read())
print(json.dumps({'id':data['id'],'url':data['url'],'title':data['title']}))
" 2>/dev/null || echo '{}')
    if [[ "$pub_json" != "{}" ]]; then
      record_published "$md" "$pub_json" | tee -a "$LOG_FILE"
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