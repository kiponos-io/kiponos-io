#!/usr/bin/env bash
# Create local cover JPEGs for Waves 15-21 articles from existing cover pool (>=20KB).
set -euo pipefail

DOCS="/home/moshe/work/kiponos-io/docs"
QUEUE="$DOCS/waves-15-21-master-queue.txt"

python3 - "$DOCS" "$QUEUE" <<'PY'
import re, shutil, sys
from pathlib import Path

docs, queue_path = Path(sys.argv[1]), Path(sys.argv[2])
pool = sorted(
    p for p in docs.glob("devto-cover-*.jpg")
    if p.stat().st_size >= 20_000 and not p.name.startswith("devto-cover-ai-")
    and not p.name.startswith("devto-cover-retail-")
    and "wave" not in p.name
)
if not pool:
    raise SystemExit("no cover pool")

# Thematic defaults (source cover basename without devto-cover- prefix)
theme = {
    "devto-ai-": "llm-serving.jpg",
    "devto-retail-": "ab-checkout.jpg",
    "devto-media-": "cdn.jpg",
    "devto-travel-": "logistics.jpg",
    "devto-pharma-": "hospital-triage.jpg",
    "devto-public-sector-": "accounting-close.jpg",
    "devto-agriculture-": "energy.jpg",
    "devto-edtech-": "qa-zero-config.jpg",
    "devto-construction-": "logistics.jpg",
    "devto-sports-": "game-server.jpg",
    "devto-aha-rabbitmq": "microservices-events.jpg",
    "devto-aha-redis": "arch-pools.jpg",
    "devto-aha-grpc": "microservices-handoff.jpg",
    "devto-aha-feign": "microservices-saga.jpg",
    "devto-aha-elasticsearch": "microservices-collab.jpg",
    "devto-aha-sqs": "automation-no-env.jpg",
    "devto-aha-graphql": "rate-limits.jpg",
    "devto-aha-oauth": "waf.jpg",
    "devto-aha-trace": "arch-observability.jpg",
    "devto-aha-quartz": "ci-testing.jpg",
    "devto-k8s-": "k8s-no-restart.jpg",
    "devto-serverless-": "automation-no-env.jpg",
    "devto-microservices-": "microservices-saga.jpg",
    "devto-arch-error": "arch-observability.jpg",
    "devto-arch-chaos": "arch-resilience.jpg",
    "devto-arch-blue": "arch-canary.jpg",
    "devto-arch-pci": "banking-aml.jpg",
    "devto-arch-hipaa": "hospital-triage.jpg",
    "devto-arch-flink": "microservices-events.jpg",
    "devto-arch-llm-token": "llm-serving.jpg",
    "devto-arch-black-friday": "rate-limits.jpg",
    "devto-vs-": "staging-profile.jpg",
    "devto-when-gitops": "arch-multi-env.jpg",
}

pool_by_name = {p.name.replace("devto-cover-", ""): p for p in pool}
created = 0

for line in queue_path.read_text().splitlines():
    line = line.strip()
    if not line or line.startswith("#"):
        continue
    md = line.split("#")[0].strip()
    md_path = docs / md
    if not md_path.exists():
        continue
    text = md_path.read_text(encoding="utf-8")
    m = re.search(r"^main_image:\s*(.+)$", text, flags=re.M)
    if not m:
        continue
    fname = m.group(1).strip().rsplit("/", 1)[-1]
    dest = docs / fname
    if dest.is_file() and dest.stat().st_size >= 20_000:
        continue
    src_name = None
    for prefix, cover in theme.items():
        if md.startswith(prefix):
            src_name = cover
            break
    src = pool_by_name.get(src_name) if src_name else None
    if not src:
        src = pool[hash(md) % len(pool)]
    shutil.copy2(src, dest)
    created += 1
    print(f"  {fname} <- {src.name}")

print(f"Covers ready: {created} new files")
PY