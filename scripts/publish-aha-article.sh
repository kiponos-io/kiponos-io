#!/usr/bin/env bash
# Publish one dev.to article + Crunchbase press ref + WhatsApp notify (Brave CDP pipeline).
set -euo pipefail

MD="${1:?Usage: publish-aha-article.sh path/to/devto-*.md}"

export BRAVE_USER_DATA_DIR="${BRAVE_USER_DATA_DIR:-$HOME/.config/crunchbase/brave-cdp-profile}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PIPELINE="$HOME/.grok/skills/devto-press-pipeline/scripts/pipeline_after_publish.py"

echo "==> Pre-publish checks"
bash "$SCRIPT_DIR/pre-publish-check.sh" "$MD"

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

echo "==> Crunchbase + WhatsApp (article id $ID)"
python3 "$PIPELINE" "$ID" --md "$MD"

python3 ~/.grok/skills/crunchbase-news/scripts/list_press_gaps.py

echo "Done: $MD (dev.to id $ID)"