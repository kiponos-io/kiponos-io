---
title: "Retry Budget Per Dependency — Stop Retry Storms Without Recycling Pods (Java SDK)"
published: false
tags: java, resilience, microservices, sre
description: Resilience4j retry limits in YAML are deploy-bound. Kiponos holds per-upstream retry budgets readable on every outbound call.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-reliability-retry-budget-per-dependency.md
main_image: https://files.catbox.moe/87bzmo.jpg
---

Wednesday 02:17 UTC. The loyalty partner API returns `503` on 94% of calls — a certificate rotation gone wrong on their side. Your checkout service still retries every failure **three times** because `maxAttempts: 3` shipped in `application-prod.yml` eighteen months ago. Outbound RPS to the partner climbs to **12,000** while inbound checkout traffic is only **1,400**. The partner SRE pings your bridge: "You're DDoSing us with retries."

The checkout lead scrolls Resilience4j config:

```yaml
resilience4j.retry:
  instances:
    loyaltyPartner:
      maxAttempts: 3
      waitDuration: 200ms
```

Cutting retries for **loyalty only** — while tax and shipping stay at three — needs a deploy. The incident commander says what the room already knows:

> "Retry budget is **operational**. Why is one failing dependency taking the whole fleet with it?"

[Kiponos.io](https://kiponos.io) holds per-dependency `max_retries` in a live tree: local reads before every outbound call, `afterValueChanged` to rebuild Resilience4j retry policies without pod recycle.

## The problem — max_retries baked into static config

Checkout calls three partners on the payment path. Each uses `@Retry` with frozen YAML:

```java
@Retry(name = "loyaltyPartner")
public LoyaltyPoints accrueLoyalty(CheckoutRequest req) {
    return loyaltyClient.accrue(req);
}
```

```yaml
resilience4j.retry:
  instances:
    loyaltyPartner:
      maxAttempts: 3
    taxService:
      maxAttempts: 3
    shippingQuotes:
      maxAttempts: 2
```

During a partner outage, you need `loyaltyPartner.max_retries: 0` **now** — not after a merge queue and rolling restart. Global retry disable is worse: tax and shipping are healthy. Static YAML forces an all-or-nothing deploy.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Retries are resilience — more is safer" | Retries **multiply** load on a failing dependency |
| "Resilience4j YAML is good enough" | YAML is per-deploy, not per-incident per-dependency |
| "Circuit breaker will stop retries" | Breaker opens **after** retry storm already landed |
| "We will scale pods to absorb retries" | Partner sees N × pods × maxAttempts — not your problem alone |
| "Idempotent POSTs mean retry freely" | Partner outage + retry storm = extended mutual pain |

## The Aha

**max_retries is operational config** — it changes per dependency during outages, peaks, and brownouts. It belongs in a **live tree** checkout reads with `getInt()` before every `@Retry` invocation, not in Resilience4j YAML frozen at boot.

## What Kiponos.io is for retry budgets

Profile `['checkout']['prod']['retry']` hydrates per-dependency retry limits into every checkout pod. Dashboard edit on `retry/loyalty_partner/max_retries` sends a **delta**; the next `accrueLoyalty()` call reads the new budget locally.

`kiponos.path("retry", "loyalty_partner").getInt("max_retries")` is a **local memory read** on the outbound hot path — no HTTP to a config service between checkout and partner.

`afterValueChanged` rebuilds the Resilience4j `RetryRegistry` entry when ops changes any `retry/*` key — same pattern as live circuit breaker tuning.

Honest boundary: Kiponos does **not** replace idempotency key design, partner SLA contracts, or service mesh retry policies. It owns **per-dependency retry floats** your Java service enforces on every call.

## Architecture

![Architecture diagram](https://files.catbox.moe/ghlasf.png)

## Config tree

```yaml
retry/
  loyalty_partner/
    max_retries: 3
    wait_duration_ms: 200
    enabled: true
  tax_service/
    max_retries: 3
    wait_duration_ms: 150
    enabled: true
  shipping_quotes/
    max_retries: 2
    wait_duration_ms: 300
    enabled: true
  incident/
    partner_outage_mode: false
    default_max_retries_during_outage: 0
  ops/
    owner: checkout-oncall
    notes: "Cut loyalty retries first — partner cert rotations are frequent"
```

## Integration (Spring Boot 3 + Resilience4j)

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

```java
@Service
public class CheckoutPartnerGateway {

    private final Kiponos kiponos;
    private final LoyaltyClient loyaltyClient;
    private final RetryRegistry retryRegistry;
    private volatile Retry loyaltyRetry;

    public CheckoutPartnerGateway(Kiponos kiponos, LoyaltyClient loyaltyClient,
                                  RetryRegistry retryRegistry) {
        this.kiponos = kiponos;
        this.loyaltyClient = loyaltyClient;
        this.retryRegistry = retryRegistry;
        kiponos.afterValueChanged(this::onRetryConfigChange);
        loyaltyRetry = rebuildLoyaltyRetry();
    }

    public LoyaltyPoints accrueLoyalty(CheckoutRequest req) {
        var cfg = kiponos.path("retry", "loyalty_partner");
        if (!cfg.getBool("enabled", true)) {
            return loyaltyClient.accrue(req);
        }
        int maxRetries = resolveMaxRetries("loyalty_partner", cfg);
        return loyaltyRetry.executeSupplier(() -> loyaltyClient.accrue(req));
    }

    private void onRetryConfigChange(ValueChange change) {
        if (change.path().startsWith("retry/")) {
            loyaltyRetry = rebuildLoyaltyRetry();
            log.warn("Loyalty retry policy rebuilt: max_retries={}",
                    resolveMaxRetries("loyalty_partner",
                            kiponos.path("retry", "loyalty_partner")));
        }
    }

    private Retry rebuildLoyaltyRetry() {
        var cfg = kiponos.path("retry", "loyalty_partner");
        int maxAttempts = resolveMaxRetries("loyalty_partner", cfg) + 1;
        return retryRegistry.retry("loyaltyPartner", RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(cfg.getInt("wait_duration_ms", 200)))
                .build());
    }

    private int resolveMaxRetries(String dependency, ConfigPath cfg) {
        if (kiponos.path("retry", "incident").getBool("partner_outage_mode", false)) {
            return kiponos.path("retry", "incident")
                    .getInt("default_max_retries_during_outage", 0);
        }
        return cfg.getInt("max_retries", 3);
    }
}
```

Ops enables `partner_outage_mode` or sets `loyalty_partner/max_retries: 0`. Outbound RPS to the failing partner drops within seconds — tax and shipping retries untouched.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Partner outage — cut retries to failing dependency only | PR + rolling restart all checkout pods | `loyalty_partner/max_retries: 0` live |
| Partial degradation — partner at 50% success | Retry storm keeps failure rate high | Lower to `max_retries: 1` mid-incident |
| Recovery — partner green again | Second deploy to restore defaults | Disable `partner_outage_mode` in one edit |
| Load test — validate zero-retry path | Feature branch per retry value | Hub profile `staging/retry` |
| Postmortem audit | Git blame on shared resilience YAML | Hub log: who set loyalty to 0 at 02:19 |

## Performance on the outbound hot path

- **`getInt()` before `executeSupplier`** — microseconds vs partner HTTP RTT
- **Retry registry rebuild on change only** — not per checkout request
- **One WebSocket per checkout pod** — not Redis coordination per retry decision
- **Delta patch** — `max_retries` 3 → 0 sends one integer across the fleet
- **Per-dependency trees** — cut loyalty without touching tax/shipping YAML sprawl

## Compare to alternatives

| Approach | Cut retries for one partner | Hot-path read cost |
|----------|----------------------------|-------------------|
| Resilience4j YAML | PR + rolling restart | Zero (frozen) |
| `@RefreshScope` + actuator | Context refresh | Bean recycle under load |
| Global circuit breaker only | Opens after storm | N/A |
| Redis-shared retry counter | Yes | RTT per outbound call |
| **Kiponos SDK** | **Dashboard, seconds** | **Memory read** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Idempotency key design and dedup storage | Application schema + docs |
| Service mesh automatic retry policy | Istio/Linkerd VirtualService |
| Partner API contract and SLA enforcement | Legal + integration agreements |
| Which dependencies get retry wrappers at all | Git-reviewed wiring |
| Bootstrap Resilience4j instance names | Git-reviewed YAML is fine |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['checkout']['prod']['retry']`.
3. Add `io.kiponos:sdk-boot-3` and Resilience4j to checkout service.
4. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['checkout']['prod']['retry']"`.
5. Move per-dependency `max_retries` out of `application-prod.yml` into the hub tree.
6. Wire `CheckoutPartnerGateway` with `afterValueChanged` retry rebuild.
7. Staging game day: simulate partner 503s, set `max_retries: 0`, confirm outbound RPS drops **without pod restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Rate limits and circuit breakers](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-rate-limits-circuit-breakers.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*max_retries belongs in the live ops tree — not in YAML that retries your partner into a bigger outage.*