---
title: "Live Energy Grid Load Dispatch Limits (Kiponos Python SDK)"
published: true
tags: python, energy, iot, realtime
description: Adjust demand-response thresholds and generator dispatch caps in Python grid orchestration without redeploy. Local SDK reads on control loops.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-energy-grid.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-energy.jpg
---

Heat waves and supply shocks require **minute-by-minute** dispatch limit changes. Python orchestrators read Kiponos locally:

```python
max_export = kiponos.path("grid", zone).get_float("max_export_mw")
dr_threshold = kiponos.path("demand_response").get_float("trigger_load_pct")
```

Grid operators update limits in dashboard; control loops see new values instantly. No SCADA config file rollout.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)