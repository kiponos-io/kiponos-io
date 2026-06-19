---
title: "Tune CI Test Parallelism and Timeouts in Real Time (Kiponos Python SDK)"
published: true
tags: python, cicd, testing, realtime
description: Adjust pytest workers, timeouts, and retry policy from Kiponos during long CI runs — no pipeline YAML edits.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ci-test-tuning.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-ci-testing.jpg
---

Long CI suites run for hours. Halfway through, infra is degraded — you need **fewer parallel workers** or **longer timeouts**. Editing `pytest.ini` mid-run is impossible.

Test orchestrators read Kiponos at suite start **and** between shards:

```python
workers = kiponos.path("ci", "pytest").get_int("max_workers")
timeout = kiponos.path("ci", "pytest").get_int("case_timeout_sec")
```

Release engineering lowers `max_workers` in dashboard; remaining shards throttle without aborting the pipeline. Local reads — no GitHub API call per test.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)