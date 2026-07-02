---
title: "Retune Fraud Thresholds and Payment Routes in Real Time — No Java Restart (Kiponos SDK)"
published: true
tags: java, fintech, security, realtime
description: Change fraud scores, routing rules, and block thresholds in your payment service while transactions keep flowing. Kiponos Java SDK reads live values locally with zero latency.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-fraud-payment-routing.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-fraud-routing.jpg
---

Payment systems are the worst place for a **deploy cycle**. Fraud patterns shift hourly. Processors go degraded. A/B routing experiments need mid-day course correction. Yet most Java payment services still bake thresholds into `application.yml` and require a restart to change a single risk score.

[Kiponos.io](https://kiponos.io) fixes that: a real-time config hub where your **Java SDK** holds the latest fraud and routing values **in memory**, updated over WebSocket deltas — no restart, no redeploy, no per-transaction remote call.

## The problem: static config in a live money path

A typical card-authorization service does this on every transaction:

```java
if (riskScore > fraudThreshold) {
    routeToManualReview();
} else if (amount > highValueLimit) {
    routeToStrongAuth();
} else {
    routeToStandardProcessor();
}
```

Those thresholds (`fraudThreshold`, `highValueLimit`, processor weights) usually come from:

1. **YAML at startup** — change means rolling restart during peak traffic
2. **Database poll** — adds latency and DB load on the hot path
3. **Feature-flag SaaS** — another network hop per evaluation

The authorization path runs thousands of times per second. You need **local reads** and **async updates** — exactly what Kiponos provides.

## How Kiponos fits payment routing

![Architecture diagram](https://files.catbox.moe/0uthng.png)

1. **Connect once** at service startup — `Kiponos.createForCurrentTeam()`
2. **Organize config** under a profile like `['payments']['v2']['prod']['fraud']`
3. **Read locally** on every transaction — `kiponos.path("fraud", "thresholds").getInt("block_score")`
4. **Ops updates live** — fraud analyst raises block threshold in dashboard; next transaction sees it

## Example config tree

```yaml
fraud/
  thresholds/
    block_score: 85
    review_score: 70
    velocity_limit_per_hour: 12
  routing/
    primary_processor: stripe
    fallback_processor: adyen
    high_risk_processor: manual_review
  limits/
    high_value_usd: 5000
    crypto_enabled: false
  rules/
    country_block_list: RU,NG
    mccs_high_risk: 7995,6012
```

## Java integration (Spring Boot payment service)

```java
import io.kiponos.sdk.Kiponos;

@Service
public class PaymentRouter {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public RouteDecision route(Transaction txn, int riskScore) {
        var thresholds = kiponos.path("fraud", "thresholds");
        int blockScore = thresholds.getInt("block_score");
        int reviewScore = thresholds.getInt("review_score");

        if (riskScore >= blockScore) {
            return RouteDecision.block("score_exceeded");
        }
        if (riskScore >= reviewScore) {
            return RouteDecision.manualReview();
        }

        var routing = kiponos.path("fraud", "routing");
        String processor = routing.get("primary_processor");
        if (txn.amountUsd() > kiponos.path("fraud", "limits").getInt("high_value_usd")) {
            processor = routing.get("high_risk_processor");
        }
        return RouteDecision.approve(processor);
    }
}
```

Every `getInt()` and `get()` is a **local memory read** — no HTTP, no JDBC, no cache miss to a remote store.

Optional listener for audit logging when ops changes a threshold:

```java
kiponos.afterValueChanged(change ->
    log.info("Fraud config changed: {} → {}", change.path(), change.newValue())
);
```

## Real-world scenarios

| Scenario | Without Kiponos | With Kiponos |
|----------|-----------------|--------------|
| Fraud spike at 2 PM | Emergency deploy or accept losses | Analyst raises `block_score` in UI |
| Processor outage | Flip YAML, restart pods | Switch `primary_processor` live |
| Black Friday limits | Pre-provision 3 config versions | Bump `high_value_usd` during event |
| New BIN attack pattern | Wait for next release | Add MCC/country rules in dashboard |

## Performance: why payments teams care

- **One WebSocket** per JVM — not one config fetch per transaction
- **Reads are O(1)** on the SDK cache — microseconds, not milliseconds
- **Delta updates** — changing `block_score` from 85 → 90 sends one patch, not the full tree
- **No GC pressure** from parsing YAML on every request

In load tests against typical authorization paths, Kiponos reads are noise compared to network I/O to card networks.

## Compare to alternatives

| Approach | Mid-flight changes | Read latency | Audit trail |
|----------|-------------------|--------------|-------------|
| Static YAML | No | Zero | Git history |
| DB config table | Yes | DB round-trip | DB logs |
| Redis cache | Yes | Cache RTT + invalidation | Custom |
| **Kiponos SDK** | **Yes** | **Zero (local)** | **Dashboard + listeners** |

## Getting started

1. [Free TeamPro at kiponos.io](https://kiponos.io) — create `payments` / `fraud` profile
2. Add `io.kiponos:sdk-boot-3` to your Spring Boot service
3. Wire `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos=...` profile
4. Replace hard-coded thresholds with `kiponos.path(...).get*()` calls
5. Run a shadow transaction, change `block_score` in the dashboard, run again — route changes instantly

Runnable golden example and Agent Skills: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

The same pattern applies to **API rate limits**, **circuit breaker thresholds**, and **A/B checkout weights** — any Java service that must change behavior at runtime without a deployment window.

---

*Kiponos.io — real-time config for Java and Python. Tune fraud rules and payment routes while money keeps moving.*