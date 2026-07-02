---
title: "Orchestrate Saga Compensation Timeouts in Real Time (Kiponos Java SDK)"
published: true
tags: java, microservices, distributed, realtime
description: Tune saga step timeouts, retry budgets, and compensation triggers across Java services without redeploy. Shared Kiponos tree with zero-latency local reads on every step.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-saga.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-microservices-saga.jpg
---

A checkout saga spans inventory, payment, shipping, and loyalty. Downstream latency shifts every hour. Black Friday is not the day to discover your **payment step timeout** is baked into `application.yml` across twelve Spring Boot services.

[Kiponos.io](https://kiponos.io) gives every saga participant the **same live orchestration parameters** — step timeouts, retry budgets, compensation triggers — via one shared config tree. Each JVM reads locally on every saga step; ops adjusts once in the dashboard; WebSocket deltas propagate without redeploying the fleet.

## Why sagas break with static config

Typical saga coordinator code:

```java
if (step.elapsedMs() > 8000) {
    compensate("payment", sagaId);
}
```

That `8000` usually comes from:

1. **Per-service YAML** — payment service says 8s, inventory says 12s; nobody agrees during an incident
2. **Env vars in Helm** — change means rolling twelve deployments
3. **Shared DB config table** — poll per step adds latency and coupling

Saga steps are **high-frequency reads** inside workflow engines. You need local memory reads and async updates — the same contract as [live API rate limits](https://dev.to/kiponos/change-api-rate-limits-and-circuit-breakers-at-runtime-no-java-redeploy-kiponos-sdk-3d94).

## Architecture: one tree, many participants

![Architecture diagram](https://files.catbox.moe/oszpym.png)

Every participant connects to profile `['orders']['v2']['prod']['sagas']`. When NOC extends `payment.step_timeout_ms`, **all JVMs** see the new value on the next step — no config server poll, no inter-service "what is timeout now?" REST calls.

## Shared saga config tree

```yaml
sagas/
  checkout/
    payment/
      step_timeout_ms: 8000
      max_retries: 2
      retry_backoff_ms: 500
      compensate_on_timeout: true
    inventory/
      step_timeout_ms: 5000
      max_retries: 3
      hold_ttl_seconds: 120
    shipping/
      step_timeout_ms: 12000
      fallback_carrier: ups_ground
    global/
      saga_ttl_minutes: 30
      alert_on_compensation: true
```

Platform ops edits **one folder**; payment, inventory, and shipping services each read **their** subtree locally.

## Java integration (saga participant)

```java
import io.kiponos.sdk.Kiponos;

@Component
public class PaymentSagaStep {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public StepResult execute(SagaContext ctx) {
        var cfg = kiponos.path("sagas", "checkout", "payment");
        int timeoutMs = cfg.getInt("step_timeout_ms");
        int maxRetries = cfg.getInt("max_retries");

        return withTimeout(timeoutMs, () -> capturePayment(ctx))
            .onTimeout(() -> cfg.getBool("compensate_on_timeout")
                ? compensate(ctx) : StepResult.retry(maxRetries));
    }
}
```

`getInt()` is a **local cache lookup** — safe inside the saga executor hot path.

Optional audit when ops changes timeouts mid-incident:

```java
kiponos.afterValueChanged(change ->
    log.warn("Saga config changed: {} → {}", change.path(), change.newValue())
);
```

## Real-world scenarios

| Scenario | Without Kiponos | With Kiponos |
|----------|-----------------|--------------|
| Card processor slow | Emergency Helm values + 12 rollouts | Bump `payment.step_timeout_ms` once |
| Warehouse API degraded | Compensations fire too early | Extend `inventory.step_timeout_ms` live |
| Carrier outage | Deploy new fallback routing | Set `shipping.fallback_carrier` in UI |
| Post-mortem tuning | Ticket + next sprint | Adjust `retry_backoff_ms` during replay tests |

## Compensation policy without redeploy

Compensation is not just timeouts — trigger thresholds can live in the same tree:

```java
boolean shouldCompensate = kiponos.path("sagas", "checkout", "global")
    .getBool("alert_on_compensation");
int sagaTtl = kiponos.path("sagas", "checkout", "global")
    .getInt("saga_ttl_minutes");
```

Risk and ops teams tune **how aggressive** the saga is while traffic is live.

## Performance

- **One WebSocket** per JVM — not a config fetch per saga step
- **Reads are O(1)** on the SDK cache — microseconds per step
- **Delta patches** — changing one timeout does not reload the full tree
- **No DB poll** on the workflow hot path

## Compare to alternatives

| Approach | Cross-service consistency | Mid-incident change | Read latency |
|----------|---------------------------|---------------------|--------------|
| Per-service YAML | Drift guaranteed | Rolling restart fleet | Zero after restart |
| Central DB config | Possible | DB round-trip per read | Milliseconds |
| Redis pub/sub | Custom glue | Invalidation complexity | Cache RTT |
| **Kiponos shared tree** | **Single source of truth** | **Dashboard edit** | **Zero (local)** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — one profile for `sagas/checkout/*`
2. Add `io.kiponos:sdk-boot-3` to each saga participant
3. Wire `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos=...` on every service
4. Replace hard-coded timeouts with `kiponos.path("sagas", ...).getInt(...)`
5. Run a chaos test — slow payment mock, extend timeout in dashboard, watch compensations stop misfiring

Runnable golden example and Agent Skills: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Sagas share state with **handoff signals** and **event routing rules** — other microservices patterns in the same live tree: who owns the lock, which topic fires next, when to escalate to manual review.

---

*Kiponos.io — real-time config for Java. Tune distributed sagas while orders are in flight.*