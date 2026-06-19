---
title: "Cross-Service Handoff Signals and Locks in Real Time (Kiponos Python SDK)"
published: true
tags: python, microservices, realtime, architecture
description: Python microservices coordinate handoffs through a live Kiponos tree — no Redis pub/sub for config, no env vars for partner endpoints.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-handoff.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-microservices-handoff.jpg
---

Service A finishes enrichment; Service B should start scoring. Instead of webhooks for **configuration state**, use a shared Kiponos handoff namespace:

```python
if kiponos.path("handoff", job_id).get_bool("enrichment_done"):
    run_scoring(job_id)
    kiponos.path("handoff", job_id).set("scoring_done", True)
```

Reads are local. Writes propagate as deltas to all listeners. Optional `afterValueChanged` triggers in-process callbacks when a partner flips a flag.

**Collaboration without config files** — each service boots with only `KIPONOS_ID`, `KIPONOS_ACCESS`, and profile path.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)