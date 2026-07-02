#!/usr/bin/env bash
# Validate article before publish: line count, GitHub canonical, SDK scope.
set -euo pipefail

MD="${1:?Usage: pre-publish-check.sh path/to/devto-*.md}"
QUALITY_DOC="${QUALITY_DOC:-$HOME/.grok/skills/devto-press-pipeline/references/ARTICLE_QUALITY_STANDARD.md}"
MIN_LINES="${MIN_LINES:-160}"
fn=$(basename "$MD")

err() { echo "pre-publish-check: $*" >&2; exit 1; }

[[ -f "$MD" ]] || err "missing file: $MD"

lines=$(wc -l < "$MD")
if [[ "$fn" == devto-aha-* ]] && (( lines < MIN_LINES )); then
  err "$fn has $lines lines (minimum $MIN_LINES per $QUALITY_DOC)"
fi

canonical=$(grep -m1 '^canonical_url:' "$MD" | sed 's/^canonical_url:\s*//')
expected="https://github.com/kiponos-io/kiponos-io/blob/master/docs/$fn"
if [[ "$canonical" != "$expected" ]]; then
  err "canonical_url must be GitHub blob for this file:\n  expected: $expected\n  got:      ${canonical:-<missing>}"
fi

# SDK scope: Java (Spring Boot 2/3) + Python only — no other language SDK articles
if grep -qiE '\b(node\.?js|typescript|\.NET|csharp|kotlin sdk|golang|go sdk|rust sdk|elixir|php laravel)\b' "$MD"; then
  err "article mentions unsupported language SDK — Kiponos ships Java (Spring Boot 2/3) and Python only"
fi

if ! grep -q 'github.com/kiponos-io/kiponos-io' "$MD"; then
  err "article must link to github.com/kiponos-io/kiponos-io at least once"
fi

if grep -q 'dev\.to/kiponos/getting-started-with-kiponosio-p5k' "$MD"; then
  : # product tour — dev.to-only, allowed
fi

echo "OK: $fn ($lines lines, GitHub canonical)"