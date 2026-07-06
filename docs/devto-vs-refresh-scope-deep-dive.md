---
title: "Beyond @RefreshScope — Why Spring Context Refresh Fails Live Ops (Deep Dive + Kiponos)"
published: false
tags: java, springboot, architecture, devops
description: "@RefreshScope recreates beans. That is fine for some wiring — catastrophic for pools, filters, and Resilience4j under load. Deep dive into Spring refresh mechanics and the Kiponos pattern for zero-latency operational reads."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-vs-refresh-scope-deep-dive.md
main_image: https://files.catbox.moe/ffnuj2.jpg
---

Sunday 03:17. Checkout is red. Hikari pool saturation after a downstream latency spike. The on-call engineer knows the fix: drop `maximum-pool-size` from 32 to 20 and tighten `resilience4j.circuitbreaker.instances.checkout.failureRateThreshold`. They merge `application-prod.yml`, hit `/actuator/refresh`, and watch **p99 jump another 400ms** while `@RefreshScope` tears down beans mid-traffic.

Staff engineer in Slack:

> "Refresh is not free. We just **recreated half the context** during a pool exhaustion incident."

Spring Boot 3 with `@ConfigurationProperties`, Spring Cloud Bus, and `@RefreshScope` is the default **live config** story in Java land. It works for **occasional wiring changes**. It fails for **operational knobs** read on every request and bound to infrastructure objects (pools, breakers, Tomcat threads) that do not tolerate async bean recreation under load.

[Kiponos.io](https://kiponos.io) separates **bootstrap wiring** (still in Spring config) from **operational reads** (SDK local cache + `afterValueChanged` for binds). No context refresh on the incident path.

## The problem — what @RefreshScope actually does

```java
@RefreshScope
@ConfigurationProperties(prefix = "resilience4j.circuitbreaker.instances.checkout")
@Component
public class CheckoutCircuitProps {
    private float failureRateThreshold = 50f;
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);
    // getters / setters
}
```

When `/actuator/refresh` fires or Spring Cloud Bus broadcasts:

1. Spring marks `@RefreshScope` beans **destroyed**
2. Next access **recreates** the bean from updated Environment
3. **Dependents** that cached the old bean reference may stay stale unless also `@RefreshScope`
4. **Connection pools**, **Resilience4j registries**, and **filters** may hold state from the pre-refresh instance

Hot-path code often does this:

```java
@Component
public class CheckoutGateway {

    private final CheckoutCircuitProps props;  // injected once at startup

    public boolean allowCheckout() {
        float threshold = props.getFailureRateThreshold();  // stale until bean recreated
        return breaker.failureRate() < threshold / 100f;
    }
}
```

Even after refresh, if `CheckoutGateway` is not `@RefreshScope`, it may hold the **old** props reference. Teams add `@RefreshScope` everywhere — and context churn spreads.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "`/actuator/refresh` updates config live" | It **recreates scoped beans** — not the same as hot-path float update |
| "Cloud Bus makes refresh instant" | Instant broadcast ≠ safe under **saturated JVM** |
| "@RefreshScope on everything fixes staleness" | Everything scoped → **mass bean recreation** on each tweak |
| "ConfigurationProperties is type-safe live config" | Type-safe **binding** — lifecycle still tied to Spring context |
| "We only refresh during maintenance windows" | Incidents do not respect maintenance windows |

## The Aha

**Operational knobs should not ride Spring bean lifecycle.** Read thresholds from an SDK local cache on the hot path; apply infrastructure binds (pool resize, breaker reset) in `afterValueChanged` on a background thread. `@RefreshScope` stays for rare bootstrap changes — not for fraud `block_score` at 3 AM.

## What Kiponos.io changes in Spring architecture

Kiponos SDK holds operational trees in memory outside Spring's refresh machinery:

- Connect once — WebSocket to hub
- `getInt()` / `getFloat()` — **local read** in filters and services
- `afterValueChanged` — explicit, controlled updates to Hikari, Logback, Resilience4j
- Profile path: `['checkout']['sb3']['prod']['live']`

Spring still owns `DataSource` bean creation. Kiponos owns **the integers you mutate** without recycling the bean graph.

## Architecture — refresh scope vs SDK read path

![Architecture diagram](https://files.catbox.moe/37q2nu.png)

## Config tree — operational layer

```yaml
checkout_ops/
  tomcat/
    max_threads: 180
    accept_count: 120
  hikari/
    maximum_pool_size: 20
    connection_timeout_ms: 2500
    idle_timeout_ms: 600000
  resilience/
    checkout/
      failure_rate_threshold: 40
      wait_duration_open_ms: 25000
      sliding_window_size: 50
  limits/
    default/
      rpm: 2000
    vip_tenant/
      rpm: 8000
  fraud/
    block_score: 86
    review_score: 71
```

Bootstrap `application.yml` keeps only:

```yaml
kiponos:
  team-id: ${KIPONOS_ID}
  access-key: ${KIPONOS_ACCESS}
  profile-path: "['checkout']['sb3']['prod']['live']"
spring:
  datasource:
    url: jdbc:postgresql://checkout-db:5432/checkout
```

## Java integration — hot path without @RefreshScope

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
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CheckoutRateLimitFilter extends OncePerRequestFilter {

    private final Kiponos kiponos;

    public CheckoutRateLimitFilter(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String tenant = req.getHeader("X-Tenant-Id");
        int rpm = kiponos.path("checkout_ops", "limits", tenantOrDefault(tenant))
                .getInt("rpm", 2000);
        if (exceeded(tenant, rpm)) {
            res.setStatus(429);
            return;
        }
        chain.doFilter(req, res);
    }
}
```

No `@RefreshScope` on the filter. `rpm` updates via delta — next request reads new value.

## LiveOpsBinder — controlled infrastructure updates

```java
@Component
public class LiveOpsBinder {

    private final Kiponos kiponos;
    private final HikariDataSource dataSource;
    private final CircuitBreakerRegistry breakers;

    public LiveOpsBinder(Kiponos kiponos, DataSource dataSource,
                         CircuitBreakerRegistry breakers) {
        this.kiponos = kiponos;
        this.dataSource = (HikariDataSource) dataSource;
        this.breakers = breakers;
        kiponos.afterValueChanged(this::onChange);
    }

    private void onChange(ValueChange change) {
        if (change.path().startsWith("checkout_ops/hikari")) {
            int max = kiponos.path("checkout_ops", "hikari").getInt("maximum_pool_size");
            dataSource.setMaximumPoolSize(max);
        }
        if (change.path().startsWith("checkout_ops/resilience/checkout")) {
            breakers.circuitBreaker("checkout").reset();
        }
    }
}
```

This is **surgical** — not `/actuator/refresh` nuking half the context.

## Python sidecar service — same ops tree, no Spring

Checkout fraud scorer on Python should not depend on Spring refresh:

```python
import os
from kiponos import Kiponos

os.environ["KIPONOS_PROFILE"] = "['checkout']['sb3']['prod']['live']"
kiponos = Kiponos.create_for_current_team()

def checkout_failure_threshold() -> float:
    return kiponos.path("checkout_ops", "resilience", "checkout").get_float(
        "failure_rate_threshold", 50.0
    )

def block_score() -> int:
    return kiponos.path("checkout_ops", "fraud").get_int("block_score", 86)
```

## Real scenarios

| Event | @RefreshScope path | Kiponos path |
|-------|-------------------|--------------|
| DB latency — shrink pool | YAML + refresh → context churn | `hikari/maximum_pool_size` → `setMaximumPoolSize` |
| Raise VIP tenant RPM | Refresh all scoped limit beans | `limits/vip_tenant/rpm` — filter reads next request |
| Circuit threshold during brownout | Resilience4j YAML + refresh | Delta + `breaker.reset()` in binder |
| Forgot @RefreshScope on consumer | **Silent stale config** | SDK tree always current on `getInt()` |
| Load test week — 20 knob changes | 20 refreshes or 20 PRs | Dashboard edits, same binary |

## Performance — refresh vs local read under load

- **Context refresh** — bean destroy/create; GC pressure; pause risk on saturated JVM
- **Kiponos hot-path read** — in-memory tree lookup; no Spring proxy recreation
- **Delta merge** — background WebSocket thread; request threads unaffected
- **Hikari resize** — explicit `setMaximumPoolSize` — not pool bean replacement
- **Virtual threads (SB3)** — Tomcat knobs still matter; tune `checkout_ops/tomcat/*` live

## Honest comparison table

| Criterion | @RefreshScope + Actuator | Kiponos SDK | Honest verdict |
|-----------|--------------------------|-------------|----------------|
| Update mechanism | Bean recreation | Delta + local read | Kiponos for ops frequency |
| Hot-path read cost | Property via bean (if not stale) | **Direct cache** | Kiponos on filters |
| Pool / breaker updates | Risky implicit recreation | **Explicit binder** | Kiponos for infra binds |
| Stale reference bugs | Common without scoping everything | Single SDK tree | Kiponos simpler mental model |
| Git-reviewed bootstrap | **Native Spring** | Bootstrap keys in YAML | Spring for wiring |
| Spring Cloud Bus broadcast | **Built-in** | WebSocket per process | Different transport — both async |
| Non-Spring services | No story | **Python SDK** | Kiponos for polyglot |

## When not to use Kiponos

| Use case | Better tool |
|----------|-------------|
| Changing `spring.datasource.url` | Git + redeploy or controlled refresh |
| Adding a new `@Bean` to context | Code deploy |
| Secrets rotation | Vault + restart policy |
| Rare quarterly wiring changes | `@RefreshScope` may suffice |

## Getting started (15 minutes) — peel ops off RefreshScope

1. Audit `@RefreshScope` beans — tag each key **bootstrap** vs **operational**.
2. [TeamPro at kiponos.io](https://kiponos.io) — profile `['checkout']['sb3']['prod']['live']`.
3. Move **three ops keys**: Hikari max, one circuit threshold, one tenant RPM.
4. Wire `CheckoutRateLimitFilter` + `LiveOpsBinder`; remove `@RefreshScope` from those props.
5. Game day: dashboard tweak vs `/actuator/refresh` — compare p99 and time-to-mitigate.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Spring Boot beyond RefreshScope](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-springboot-beyond-refresh-scope.md)
- [JDBC pool tuning live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-connection-pool-live.md)
- [Tomcat maxThreads Aha](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tomcat-threads.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — @RefreshScope for rare wiring. Local SDK reads for how checkout behaves at 3 AM.*