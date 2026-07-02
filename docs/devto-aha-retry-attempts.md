---
title: "@Retryable(maxAttempts=3) Should Not Require a Release — Change Retry Policy Live (Spring Boot)"
published: false
tags: java, springboot, resilience, devops
description: Retry counts and backoff delays feel like compile-time philosophy. When a partner API flakes, maxAttempts=3 is operational — not architecture. Kiponos feeds live retry policy with zero-latency reads.
canonical_url: https://dev.to/kiponos/retryablemaxattempts3-should-not-require-a-release-change-retry-policy-live-spring-boot-35b
main_image: https://files.catbox.moe/w2vd1k.jpg
---

Tuesday 2:14 AM. Your payment adapter throws `503` from a partner gateway. Spring Retry exhausts **three attempts** — because `@Retryable(maxAttempts = 3)` was copied from a blog post in 2022 and nobody has touched it since.

Orders fail. Finance pings Slack. Someone asks to "just bump retries to seven" until the partner recovers.

The senior engineer on call sighs:

> "Retry policy is **code**. We need a PR, review, and deploy."

But retries are not philosophy. They are **operational tolerance** — how hard you lean on a sick dependency **right now**. Treating `maxAttempts` like a `@Deprecated` annotation is how teams lose revenue while a one-line constant waits in CI.

**The Aha:** retry limits and backoff delays can live in [Kiponos.io](https://kiponos.io) and be read **locally on every attempt** — change them from the dashboard while the JVM keeps retrying other traffic. No redeploy. No restart. No actuator refresh.

## Step 1 — The hard-coded belief

```java
@Retryable(
    retryFor = {TransientPartnerException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 500, multiplier = 2.0)
)
public PaymentResult charge(ChargeRequest req) {
    return partnerClient.charge(req);
}
```

Or worse — manual retry loops:

```java
private static final int MAX_ATTEMPTS = 3;
private static final long BASE_DELAY_MS = 500;
```

| Belief | Reality |
|--------|---------|
| "Three retries is industry standard" | Partner outages last 45 minutes |
| "Backoff belongs in code for clarity" | Clarity does not help at 2 AM |
| "We'll tune retries next sprint" | Declines cost money per minute |

Senior developers understand exponential backoff. They do not realize **the integers can be operational config** — same class of problem as rate limits and circuit thresholds.

## Step 2 — The Aha: live retry policy on the hot path

Move policy into the hub:

```yaml
retry/
  partner_payments/
    max_attempts: 3
    base_delay_ms: 500
    multiplier: 2.0
    max_delay_ms: 8000
    enabled: true
```

Read locally inside your retry executor — **zero network on each attempt**:

```java
@Component
public class LiveRetryExecutor {

    private final Kiponos kiponos;
    private final PartnerClient partner;

    public LiveRetryExecutor(Kiponos kiponos, PartnerClient partner) {
        this.kiponos = kiponos;
        this.partner = partner;
    }

    public PaymentResult chargeWithLivePolicy(ChargeRequest req) {
        var policy = kiponos.path("retry", "partner_payments");
        if (!policy.getBool("enabled", true)) {
            return partner.charge(req);
        }
        int max = policy.getInt("max_attempts", 3);
        long delay = policy.getLong("base_delay_ms", 500);
        double mult = policy.getDouble("multiplier", 2.0);
        long maxDelay = policy.getLong("max_delay_ms", 8000);

        TransientPartnerException last = null;
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                return partner.charge(req);
            } catch (TransientPartnerException e) {
                last = e;
                if (attempt == max) break;
                sleep(Math.min((long) (delay * Math.pow(mult, attempt - 1)), maxDelay));
            }
        }
        throw last;
    }
}
```

Partner still flapping? Ops sets `max_attempts` to `7` in the dashboard. **The next charge request** already sees seven — because `getInt()` reads the patched in-memory tree. The JVM never restarted.

## Step 3 — How it works

![Architecture diagram](https://files.catbox.moe/fp3fst.png)

1. WebSocket connects at startup.
2. Dashboard edit sends **delta only** for `max_attempts`.
3. SDK merges into local tree asynchronously.
4. Every retry loop iteration calls `getInt()` — **local read**, no Redis poll.

This is the full picture senior engineers need: not magic — **operational parameters decoupled from deployment**.

## Spring Boot integration

```java
@Configuration
public class KiponosConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey,
            @Value("${kiponos.profile-path}") String profilePath) {
        return Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath(profilePath)
                .build();
    }
}
```

Optional audit:

```java
kiponos.afterValueChanged(c ->
    log.warn("[kiponos] retry policy {} → {}", c.path(), c.newValue()));
```

## Scenarios

| Event | Hard-coded path | Live path |
|-------|-----------------|-----------|
| Partner brownout | PR + deploy for `maxAttempts` | `max_attempts: 7` in hub |
| Partner recovered | Leave inflated retries wasting latency | Drop back to `3` instantly |
| Load test | Branch per backoff curve | Profile `loadtest/retry` |
| Cost control | Retries amplify billable API calls | Toggle `enabled: false` break-glass |

Pair with [circuit breaker thresholds live](https://dev.to/kiponos/circuit-breaker-and-bulkhead-thresholds-you-can-tune-live-kiponos-java-sdk-1hai) — retries and breakers are siblings in resilience ops.

## Before / after

| Approach | Mid-outage tweak | Per-attempt read cost |
|----------|------------------|------------------------|
| `@Retryable` constants | Redeploy | Zero (but frozen) |
| `@RefreshScope` on retry bean | Actuator refresh | Bean churn |
| Poll config service | Fast dashboard | Network RTT |
| **Kiponos SDK** | **Seconds** | **Memory read** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Idempotency keys / request hashing | Application design in Git |
| Whether to retry POST at all | Code + domain rules |
| Partner SLA contract version | Git-reviewed integration module |

## Getting started

1. [kiponos.io](https://kiponos.io) — profile `['payments']['prod']['retry']`.
2. Replace one `@Retryable` hotspot with `LiveRetryExecutor`.
3. Game day: simulate partner 503s, raise `max_attempts` live, watch success rate recover without rollout.

[github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — retries are tolerance, not tattoos.*