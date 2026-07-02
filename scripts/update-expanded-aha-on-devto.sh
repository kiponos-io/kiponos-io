#!/usr/bin/env bash
# Push expanded markdown to existing dev.to article IDs (no new Crunchbase).
set -euo pipefail

IDS_JSON="/home/moshe/work/kiponos-io/docs/wave-aha-article-ids.json"
DOCS="/home/moshe/work/kiponos-io/docs"
POST=~/.grok/skills/devto/scripts/post.py
NOTIFY=/home/moshe/work/kiponos-io/scripts/notify-devto-whatsapp.sh

while IFS=$'\t' read -r md id; do
  path="$DOCS/$md"
  [[ -f "$path" ]] || continue
  echo "==> Update dev.to #$id : $md"
  OUT=$(python3 "$POST" "$path" --update "$id" --publish)
  echo "$OUT"
  TITLE=$(echo "$OUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['title'])")
  URL=$(echo "$OUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['url'])")
  TAGS=$(python3 -c "import re,pathlib; t=pathlib.Path('$path').read_text(); m=re.search(r'^tags:\s*(.+)$',t,re.M); print(m.group(1).strip() if m else '')")
  bash "$NOTIFY" "$TITLE" "$URL" "$id" "$path" "updated (expanded)" "tags: $TAGS"
  sleep 5
done < <(python3 -c "
import json
from pathlib import Path
ids = json.loads(Path('$IDS_JSON').read_text())
for md, aid in ids.items():
    print(f'{md}\t{aid}')
")

echo "All expanded articles updated on dev.to"