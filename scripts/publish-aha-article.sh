#!/usr/bin/env bash
# Publish one dev.to article + Crunchbase press ref (Brave CDP pipeline).
set -euo pipefail

MD="${1:?Usage: publish-aha-article.sh path/to/devto-aha-*.md}"

export BRAVE_USER_DATA_DIR="${BRAVE_USER_DATA_DIR:-$HOME/.config/crunchbase/brave-cdp-profile}"



echo "==> Diagram embed: $MD"
python3 << PY
import importlib.util
from pathlib import Path
spec = importlib.util.spec_from_file_location(
    "diagrams", Path.home() / ".grok/skills/devto/scripts/diagrams.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
md = Path("$MD")
text, n = mod.replace_flowchart_fences(md.read_text(encoding="utf-8"), md, force_upload=False)
md.write_text(text, encoding="utf-8")
print(f"diagrams: {n}")
PY

echo "==> Cover ensure"
python3 ~/.grok/skills/devto/scripts/cover.py "$MD" --ensure --update-md

echo "==> dev.to publish"
OUT=$(python3 ~/.grok/skills/devto/scripts/post.py "$MD" --publish)
echo "$OUT"
ID=$(echo "$OUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

echo "==> Crunchbase pipeline (article id $ID)"
~/.grok/skills/devto-press-pipeline/scripts/pipeline_after_publish.py "$ID"

python3 ~/.grok/skills/crunchbase-news/scripts/list_press_gaps.py

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
QUALITY_DOC=~/.grok/skills/devto-press-pipeline/references/ARTICLE_QUALITY_STANDARD.md
if [[ -f "$QUALITY_DOC" ]] && [[ "$MD" == *devto-aha-* ]]; then
  LINES=$(wc -l < "$MD")
  if (( LINES < 160 )); then
    echo "ERROR: $MD has $LINES lines — minimum 160 per $QUALITY_DOC" >&2
    exit 1
  fi
fi
TITLE=$(echo "$OUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['title'])")
URL=$(echo "$OUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['url'])")
TAGS=$(python3 -c "
import re, pathlib
t = pathlib.Path('$MD').read_text(encoding='utf-8')
m = re.search(r'^tags:\s*(.+)$', t, re.M)
print(m.group(1).strip() if m else '')
")
EXTRA=""
[[ -n "$TAGS" ]] && EXTRA="tags: $TAGS"
bash "$SCRIPT_DIR/notify-devto-whatsapp.sh" "$TITLE" "$URL" "$ID" "$MD" "done" "$EXTRA"

echo "Done: $MD (dev.to id $ID)"