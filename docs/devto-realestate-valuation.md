---
title: "Adjust Real-Estate Valuation Model Weights in Real Time (Kiponos Python SDK)"
published: true
tags: python, realestate, ai, realtime
description: Tune comps weights, cap rates, and market adjustment factors in Python AVM engines without redeploy. Local Kiponos reads on every valuation.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realestate-valuation.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-realestate.jpg
---

Housing markets shift weekly. AVM models need **live** comps distance weights and cap-rate adjustments:

```python
w_comps = kiponos.path("avm", market).get_float("comps_weight")
cap = kiponos.path("avm", market).get_float("cap_rate_adj")
```

Analysts update market folder in Kiponos; valuations immediately reflect new economics. No model service restart.

[kiponos.io](https://kiponos.io) · [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)