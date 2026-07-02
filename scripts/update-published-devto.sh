#!/usr/bin/env bash
# Push markdown fixes to already-published dev.to articles (--update by manifest id).
set -euo pipefail

POST_SCRIPT="${POST_SCRIPT:-$HOME/.grok/skills/devto/scripts/post.py}"
MANIFEST="${MANIFEST:-$HOME/.config/devto/published-manifest.json}"
LOG="${LOG:-$HOME/.config/devto/update-published.log}"

[[ -f "$MANIFEST" ]] || { echo "Missing manifest: $MANIFEST"; exit 1; }

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG"; }

python3 - "$MANIFEST" <<'PY' | while IFS=$'\t' read -r md aid; do
import json, sys
data = json.load(open(sys.argv[1]))
for md, info in data.items():
    aid = info.get("id")
    if aid:
        print(f"{md}\t{aid}")
PY
  [[ -f "$md" ]] || { log "SKIP missing file: $md"; continue; }
  log "Updating dev.to id=$aid file=$md"
  if out=$(python3 "$POST_SCRIPT" "$md" --update "$aid" --publish 2>&1); then
    log "OK: $out"
  else
    log "FAIL: $out"
  fi
  sleep 2
done

log "Update batch complete."