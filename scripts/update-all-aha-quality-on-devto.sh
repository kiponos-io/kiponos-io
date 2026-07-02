#!/usr/bin/env bash
# Re-push gold-standard rewrites to dev.to (diagrams + --update --publish + WhatsApp).
set -euo pipefail

DOCS=/home/moshe/work/kiponos-io/docs
POST=~/.grok/skills/devto/scripts/post.py
DIAG=~/.grok/skills/devto/scripts/diagrams.py
NOTIFY=/home/moshe/work/kiponos-io/scripts/notify-devto-whatsapp.sh

merge_ids() {
  python3 -c "
import json
from pathlib import Path
ids = {}
for p in [
    Path('$DOCS/wave-aha-article-ids.json'),
    Path('$DOCS/wave11-aha-article-ids.json'),
]:
    if p.exists():
        ids.update(json.loads(p.read_text()))
for md, aid in sorted(ids.items()):
    print(f'{md}\t{aid}')
"
}

echo "==> Render architecture diagrams"
for q in wave12 wave13 wave14; do
  while IFS= read -r line; do
    [[ -z "$line" || "$line" =~ ^# ]] && continue
    f="$DOCS/${line// /}"
    [[ -f "$f" ]] && python3 "$DIAG" "$f" || true
  done < "$DOCS/${q}-aha-publish-queue.txt"
done
for f in "$DOCS"/devto-aha-cache-ttl.md "$DOCS"/devto-aha-rate-limiter.md \
         "$DOCS"/devto-aha-batch-chunk.md "$DOCS"/devto-aha-log-levels.md \
         "$DOCS"/devto-aha-webhook-retry.md "$DOCS"/devto-aha-http-timeout.md; do
  [[ -f "$f" ]] && python3 "$DIAG" "$f" || true
done

while IFS=$'\t' read -r md id; do
  path="$DOCS/$md"
  [[ -f "$path" ]] || { echo "SKIP missing $path"; continue; }
  lines=$(wc -l < "$path")
  if (( lines < 160 )); then
    echo "WARN: $md only $lines lines — skipping" >&2
    continue
  fi
  echo "==> Update #$id ($lines lines): $md"
  OUT=$(python3 "$POST" "$path" --update "$id" --publish)
  echo "$OUT"
  TITLE=$(echo "$OUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['title'])")
  URL=$(echo "$OUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['url'])")
  TAGS=$(python3 -c "import re,pathlib; t=pathlib.Path('$path').read_text(); m=re.search(r'^tags:\s*(.+)$',t,re.M); print(m.group(1).strip() if m else '')")
  bash "$NOTIFY" "$TITLE" "$URL" "$id" "$path" "gold-standard rewrite" "tags: $TAGS"
  sleep 8
done < <(merge_ids)

echo "Quality rewrite push complete."