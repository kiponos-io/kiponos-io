---
title: "Retune Logistics Fleet Routing Parameters in Real Time (Kiponos Python SDK)"
published: true
tags: python, logistics, ops, realtime
description: Change delivery windows, route priorities, and capacity caps in Python dispatch systems without redeploy. Local Kiponos reads on every assignment.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-logistics-routing.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-logistics.jpg
---

Weather, traffic, and warehouse backlog change route economics by the hour. Python dispatch services need **live** max stops per driver, priority weights, and SLA buffers.

```python
cfg = kiponos.path("dispatch", region)
max_stops = cfg.get_int("max_stops_per_route")
priority = cfg.get_float("express_weight")
```

Ops updates region config in dashboard; assignment loop reads locally — zero network on each route solve.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)