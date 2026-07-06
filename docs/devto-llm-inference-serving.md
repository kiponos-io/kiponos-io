---
title: "Steer LLM Inference Serving in Real Time — Temperature, Routing, Limits (Kiponos Python SDK)"
published: true
tags: python, ai, llm, realtime
description: Change temperature, max tokens, model routing weights, and rate limits on live Python inference workers without restarts. Kiponos WebSocket deltas, zero-latency local reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-llm-inference-serving.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

LLM serving is never "set and forget." Costs spike, a model degrades, you need to **route 80% traffic to the backup**, **lower max tokens**, or **raise temperature** for a creative endpoint — while GPUs are hot.

[Kiponos.io](https://kiponos.io) lets Python inference workers read **serving parameters from memory** on every request.

## Request path

```python
def build_request(prompt: str, kiponos, route: str) -> dict:
    cfg = kiponos.path("serving", route)
    return {
        "model": cfg.get("model"),
        "temperature": cfg.get_float("temperature"),
        "max_tokens": cfg.get_int("max_tokens"),
        "top_p": cfg.get_float("top_p"),
    }
```

No object-store fetch. No Redis per request. Local cache read.

## Serving config tree

```yaml
serving/
  chat/
    model: gpt-4o-mini
    temperature: 0.3
    max_tokens: 2048
    top_p: 0.95
  creative/
    model: claude-sonnet
    temperature: 0.9
    max_tokens: 4096
  routing/
    primary_weight: 70
    fallback_weight: 30
    fallback_model: gpt-4o-mini
  limits/
    rpm_per_user: 60
    max_concurrent: 200
```

## Live ops scenarios

| Situation | Kiponos tweak |
|-----------|---------------|
| Primary model outage | Shift `primary_weight` → 0, fallback → 100 |
| Cost spike | Lower `max_tokens`, switch to cheaper `model` |
| Quality regression | Drop `temperature`, tighten `top_p` |
| Launch traffic | Raise `max_concurrent` and `rpm_per_user` |

## Multi-worker fleet

Every inference worker holds the same Kiponos connection profile. One dashboard change → **delta broadcast** → all workers update locally. No rolling restart across the GPU pool.

Related patterns: [ML training tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realtime-ml-training.md), [supervisor orchestration](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-supervisor-ml-training.md).

Try [kiponos.io](https://kiponos.io). Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Python. Steer inference while GPUs are running.*