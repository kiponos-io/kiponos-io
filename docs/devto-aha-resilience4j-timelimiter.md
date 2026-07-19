---
main_image: 
title: "resilience4j.timelimiter.timeoutDuration Felt Permanent — We Changed TimeLimiter Budgets Live Under Load"
published: false
tags: java, resilience4j, springboot, devops
description: Resilience4j TimeLimiter timeouts are often frozen at bean creation. When a dependency degrades, Kiponos feeds live timeoutDuration so you fail faster (or buy seconds) without a redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-resilience4j-timelimiter.md
---

Wednesday 14:07. Checkout wraps the payment client in Resilience4j `TimeLimiter` at **2s**. The acquirer is slow but alive. Your limiter cancels work that would have finished at 2.4s — and the circuit starts opening on **your** impatience, not their death.

Someone wants **3.5s** for the next hour. The number lives here:

```yaml
resilience4j.timelimiter:
  instances:
    payment:
      timeoutDuration: 2s
      cancelRunningFuture: true
```

Bean factory already built the `TimeLimiter`. New value → new pod.

> "TimeLimiter is a **resilience contract**. Change tickets only."

**The Aha:** `timeoutDuration` is an **incident dial**. With [Kiponos.io](https://kiponos.io) you store per-dependency budgets live and construct or refresh limiters from local gets — zero network on the hot path.

## Belief vs production

| Belief | Reality |
|--------|---------|
| 2s is always correct | Correct for one SLO, wrong for a partial outage |
| Cancel always | Sometimes you want observe-only during game days |
| One instance name forever | Multi-tenant products need per-tenant budgets |

## Kiponos shape

```yaml
resilience_ops/
  timelimiter/
    payment/
      timeout_ms: 2000
      cancel_running: true
    inventory/
      timeout_ms: 800
      cancel_running: true
```

## Integration sketch

```java
Duration d = Duration.ofMillis(
    kiponos.path("resilience_ops", "timelimiter", "payment")
        .getLong("timeout_ms", 2000));
// Build TimeLimiterConfig / decorate supplier with live Duration
// Refresh on afterValueChanged for that path
```

Pair with [circuit breaker thresholds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-circuit-breaker-threshold.md) and [bulkheads](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-bulkhead-concurrent.md) so budgets move as a **system**, not one knob in isolation.

## Scenarios

| Moment | Frozen | Live |
|--------|--------|------|
| Acquirer brownout | Deploy | Open payment to 3500ms for 45 minutes |
| Inventory storm | Hope | Drop inventory to 400ms |
| Load test | Hard-code | Hub profile `loadtest` |

## Before / after

| Approach | Change mid-incident | Hot path |
|----------|---------------------|----------|
| YAML beans | Redeploy | Frozen |
| Poll config server each call | Yes | Network tax |
| **Kiponos** | **Delta** | **Local get** |

## When not

| Case | Prefer |
|------|--------|
| Algorithm change (fallback chain) | Code review |
| New dependency identity | Architecture |
| Secrets in resilience config | Never live |

## Getting started

1. Lift `timeout_ms` + cancel flag into hub trees  
2. Rebuild `TimeLimiter` instances on path change  
3. Dashboards: which budget opened the circuit?  
4. Document owner per instance (`payment` vs `inventory`)  

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) · [GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)

---

*Kiponos.io — time limits are budgets, not religion.*
