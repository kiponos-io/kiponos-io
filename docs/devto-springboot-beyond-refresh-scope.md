---
title: "Spring Boot 3 Operational Config — Beyond @RefreshScope and Actuator Restarts"
published: false
tags: java, springboot, architecture, devops
description: Senior Spring teams still restart pods to change timeouts, pool sizes, and resilience thresholds. This guide maps @ConfigurationProperties limits and wires Kiponos for zero-latency reads on the request path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-springboot-beyond-refresh-scope.md
main_image: https://files.catbox.moe/ffnuj2.jpg
---

You shipped Spring Boot 3 with `@ConfigurationProperties`, Spring Cloud Config, and `@RefreshScope`. Production still pages you because **changing `resilience4j.circuitbreaker.instances.payments.failureRateThreshold` requires a context refresh** — and refresh is not free on a saturated JVM.

Senior server teams need a clean split:

- **Bootstrap wiring** — stays in Git / Spring config (URLs, credentials references, feature skeleton)
- **Operational knobs** — change during incidents without recycling beans

[Kiponos.io](https://kiponos.io) feeds **live trees** into Spring services via the Java SDK: WebSocket deltas, in-process cache, **`get()` with no network on the hot path**.

## What Spring gives you (and where it stops)

| Mechanism | Good for | Weak for live ops |
|-----------|----------|-------------------|
| `application.yml` | Defaults | PR + deploy per tweak |
| `@ConfigurationProperties` | Typed binding | Immutable until refresh |
| `@RefreshScope` + `/actuator/refresh` | Some beans | Context churn; easy to miss dependents |
| Spring Cloud Bus | Broadcast refresh | Still bean recycle; not per-request floats |
| Env vars / K8s ConfigMaps | Platform injection | Pod rollout culture |

`@RefreshScope` recreates beans. Filters, connection pools, and Resilience4j registries do not always rebound cleanly under load. **Operational tuning** (thresholds, limits, timeouts) should not ride the same lifecycle as `@Service` wiring.

## Architecture

![Architecture diagram](https://files.catbox.moe/z2kn7r.png)

```
Spring Boot service
  ├── @ConfigurationProperties → bootstrap only
  ├── Kiponos SDK (singleton) → operational tree
  └── afterValueChanged → resize pools / update breakers
```

## Config tree (operational layer)

```yaml
spring_ops/
  tomcat/
    max_threads: 200
    accept_count: 100
  hikari/
    maximum_pool_size: 24
    connection_timeout_ms: 2500
  resilience/
    payments:
      failure_rate_threshold: 50
      wait_duration_open_ms: 30000
  http_client/
    read_timeout_ms: 4000
    max_connections_per_route: 80
```

## Bootstrapping Kiponos in Spring Boot 3

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

Keep **only** team id, access key, and profile path in `application.yml` — not the operational floats.

## Hot path: zero-latency read in a filter

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantRateLimitFilter extends OncePerRequestFilter {

    private final Kiponos kiponos;

    public TenantRateLimitFilter(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String tenant = req.getHeader("X-Tenant-Id");
        int rpm = kiponos.path("limits", tenant, "rpm").getInt("value", 1200);
        if (rateExceeded(tenant, rpm)) {
            res.setStatus(429);
            return;
        }
        chain.doFilter(req, res);
    }
}
```

`getInt` hits the SDK cache — same thread, no Redis round-trip.

## React to deltas (pools & Resilience4j)

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
        if (change.path().startsWith("spring_ops/hikari")) {
            int max = kiponos.path("spring_ops", "hikari").getInt("maximum_pool_size");
            dataSource.setMaximumPoolSize(max);
        }
        if (change.path().startsWith("spring_ops/resilience/payments")) {
            var cfg = kiponos.path("spring_ops", "resilience", "payments");
            breakers.circuitBreaker("payments").reset();
            // apply new failureRateThreshold via registry rebuild or custom wrapper
        }
    }
}
```

Resize and breaker updates happen **asynchronously** when ops changes the dashboard — not when a developer merges YAML.

## Spring Boot 3 + virtual threads note

With `spring.threads.virtual.enabled=true`, **carrier thread pool** and **Tomcat accept queue** still matter under burst. Tune `spring_ops/tomcat/*` live while traffic is running instead of guessing values in `application-prod.yml` once a quarter.

## Scenarios (senior SRE table)

| Incident | Spring-only path | Kiponos path |
|----------|------------------|--------------|
| DB latency spike | Lower Hikari max in YAML → rollout | Dashboard → delta → `setMaximumPoolSize` |
| Partner launch | Raise tenant RPM | `limits/partner_x/rpm` live |
| Downstream brownout | Edit Resilience4j YAML → refresh | `resilience/payments/failure_rate_threshold` |
| Load test week | New branch per knob | Same binary, profile `loadtest/live` |

## Compare honestly

| Approach | Hot-path cost | Live incident tweak |
|----------|---------------|---------------------|
| `@RefreshScope` | Bean rebuild | Actuator refresh (risky under load) |
| Redis config | RTT per read | Fast dashboard, network on read |
| **Kiponos SDK** | **Memory read** | **Dashboard delta, local cache** |

## Anti-patterns we see in mature Spring shops

1. **100-key `@ConfigurationProperties` classes** copied per environment
2. **Actuator `/env` POST** in prod without audit
3. **Resilience4j YAML** edited only during blameless postmortems
4. **Multiple `@Profile` beans** for the same limit with drift

## Getting started (one service pilot)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['billing']['prod']['live']`
2. Move **three** operational keys out of YAML (pool max, one breaker threshold, one rate limit)
3. Wire `afterValueChanged` for pool resize
4. Leave URLs and secrets in Spring config / sealed secrets
5. Game day: measure tweak latency vs `@RefreshScope` refresh

Related: [rate limits & circuit breakers](https://dev.to/kiponos/change-api-rate-limits-and-circuit-breakers-at-runtime-no-java-redeploy-kiponos-sdk-3d94), [JDBC pools live](https://dev.to/kiponos/tune-jdbc-and-http-connection-pools-at-runtime-kiponos-java-sdk-4d2l), [GitOps boundary](https://dev.to/kiponos/gitops-vs-live-operational-config-where-kiponos-fits-in-platform-architecture-2nbb).

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Spring Boot for wiring. Live config for how it behaves under load.*