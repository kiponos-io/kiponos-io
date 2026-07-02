#!/usr/bin/env bash
# Push fixed markdown (github→dev.to links) to all published articles.
set -euo pipefail

POST_SCRIPT="${POST_SCRIPT:-$HOME/.grok/skills/devto/scripts/post.py}"
DOCS="/home/moshe/work/kiponos-io/docs"
LOG="${LOG:-$HOME/.config/devto/update-links.log}"

log() { echo "[$(date -Iseconds)] $*" | tee -a "$LOG"; }

python3 - "$DOCS" <<'PY' | while IFS=$'\t' read -r md aid; do
import json, sys
from pathlib import Path

docs = Path(sys.argv[1])
ids: dict[str, int] = {}

for jf in ("wave-aha-article-ids.json", "wave11-aha-article-ids.json"):
    p = docs / jf
    if p.exists():
        for fn, aid in json.loads(p.read_text()).items():
            ids[fn] = int(aid)

manifest = json.loads((Path.home() / ".config/devto/published-manifest.json").read_text())
for path, info in manifest.items():
    fn = Path(path).name
    if info.get("id"):
        ids[fn] = int(info["id"])

extra = {
    "devto-mind-reader-live-ops.md": 4045312,
    "devto-getting-started-developer-guide.md": 4047302,
    "devto-springboot-beyond-refresh-scope.md": 4044940,
    "devto-arch-gitops-vs-live-config.md": 4044900,
    "devto-arch-opentelemetry-slo-thresholds.md": 4052935,
    "devto-arch-multi-region-active-active.md": 4052951,
    "devto-arch-config-schema-versioning.md": 4052960,
    "devto-arch-sidecar-vs-embedded-sdk.md": 4053762,
    "devto-arch-disaster-recovery-live-config.md": 4053786,
    "devto-arch-cost-control-runtime.md": 4053769,
}
# fix gitops id typo
extra["devto-arch-gitops-vs-live-config.md"] = 4044906
ids.update(extra)

for fn in sorted(ids):
    md = docs / fn
    if md.is_file():
        print(f"{md}\t{ids[fn]}")
PY
  [[ -f "$md" ]] || { log "SKIP missing: $md"; continue; }
  log "Updating id=$aid $(basename "$md")"
  if out=$(python3 "$POST_SCRIPT" "$md" --update "$aid" --publish 2>&1); then
    log "OK"
  else
    log "FAIL: $out"
  fi
  sleep 2
done

log "Update-all complete."