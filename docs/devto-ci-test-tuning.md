---
title: "Tune CI Test Parallelism and Timeouts Live (Kiponos Python SDK)"
published: true
tags: python, cicd, testing, devops
description: Control pytest shard count, per-suite timeouts, and retry policy from a Kiponos profile. Change CI behavior mid-pipeline without editing workflow YAML.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ci-test-tuning.md
main_image: https://files.catbox.moe/ifp0w3.jpg
---

CI pipelines are frozen at commit time. Workflow YAML says `parallelism: 8`, `timeout-minutes: 45`, `retry: 2`. Then QA cluster gets slow, flaky tests multiply, and you are **pushing YAML commits** to unblock a release while the pipeline is still running.

[Kiponos.io](https://kiponos.io) externalizes **runtime CI knobs** into a live profile your test workers read locally: shard counts, suite timeouts, retry backoff, and "disable this shard" flags — adjustable from the dashboard while jobs execute.

## Static CI config breaks down

```yaml
# .github/workflows/e2e.yml — baked at push time
strategy:
  matrix:
    shard: [0, 1, 2, 3, 4, 5, 6, 7]
timeout-minutes: 45
```

If shard 3 hammers a degraded dependency, you cannot **turn off shard 3** without a new commit. If the cluster is healthy and you want **16 shards** for a release candidate, same problem.

## Live CI profile

```yaml
ci/
  e2e/
    shard_count: 8
    shard_timeout_min: 45
    max_retries: 2
    retry_backoff_sec: 10
    disabled_shards: ""
  load/
    reduce_parallelism: false
    max_in_flight_requests: 200
suites/
  contract/
    enabled: true
    timeout_min: 15
```

## Python worker reads config each shard start

```python
from kiponos import Kiponos

kiponos = Kiponos.create_for_current_team()
cfg = kiponos.path("ci", "e2e")

shard_id = int(os.environ["SHARD_ID"])  # only identity in env — not behavior
disabled = {int(x) for x in cfg.get("disabled_shards", "").split(",") if x}

if shard_id in disabled:
    sys.exit(0)  # graceful no-op shard

timeout_sec = cfg.get_int("shard_timeout_min") * 60
max_retries = cfg.get_int("max_retries")
```

Ops adds `3` to `disabled_shards` during incident — shard 3 exits cleanly; other shards continue.

## Dynamic parallelism without re-pushing workflow

Orchestrator script (runs once per pipeline):

```python
shard_count = kiponos.path("ci", "e2e").get_int("shard_count")
for i in range(shard_count):
    spawn_job(shard_id=i)
```

Increase `shard_count` to `12` in Kiponos **before** nightly run starts — orchestrator picks it up. No workflow edit.

## Real-world scenarios

| Scenario | Live tweak |
|----------|------------|
| QA DB slow | Lower `shard_count`, raise `shard_timeout_min` |
| One shard toxic | Add shard id to `disabled_shards` |
| Release crunch | Raise `shard_count` for faster feedback |
| Contract suite blocking | `suites/contract/enabled: false` |

## Performance

Shard startup reads config once; inner test loops use same profile for [automation toggles](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-automation-no-env.md). All `get_int()` calls are **local**.

## Compare to alternatives

| Approach | Tune while pipeline runs | Non-developer can adjust |
|----------|--------------------------|--------------------------|
| Workflow YAML only | No | No |
| Re-run with inputs | Manual | Limited |
| External orchestrator DB | Custom build | Custom UI |
| **Kiponos CI profile** | **Yes** | **Dashboard** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — `['ci']['v1']['github']['e2e']`
2. Move parallelism/timeouts from YAML into Kiponos tree
3. CI secrets: only `KIPONOS_ID`, `KIPONOS_ACCESS`, `SHARD_ID`
4. Disable one shard live; confirm pipeline completes without redeploy

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Same hub supports **staging profile overrides** and **K8s pod config** — one lifecycle story from laptop to production.

---

*Kiponos.io — real-time config for Python. CI that adapts while the pipeline is running.*