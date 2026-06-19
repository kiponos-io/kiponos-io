#!/usr/bin/env bash
# Package golden/java for GitHub Releases (excludes build output and local secrets).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/golden-java.zip}"

cd "$ROOT/golden/java"
zip -r "$OUT" . \
  -x 'build/*' -x '.gradle/*' -x 'kiponos.local.gradle' -x '*.iml'

echo "Created $OUT"