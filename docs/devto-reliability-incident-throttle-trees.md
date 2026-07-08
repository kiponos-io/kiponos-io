---
title: "Incident Throttle Trees — Coordinated Rate Limits Across Services (Java SDK)"
published: false
tags: java, sre, rate-limit, architecture
description: Per-service throttles during incidents diverge without a shared tree. Kiponos coordinates throttle knobs across JVMs in one profile.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-reliability-incident-throttle-trees.md
main_image: https://files.catbox.moe/87bzmo.jpg
---

Friday 16:48 UTC. A botnet shifts from credential stuffing to **search API flooding** — 340,000 RPM against `/api/v1/search` while legitimate checkout traffic holds steady at 2,100 RPM. Edge WAF rules lag twenty minutes behind the attack pattern. Individual service owners start editing local constants:

- Gateway team: `GLOBAL_RPM = 8000` in `RateLimitFilter.java`
- Search team: `SEARCH_RPM = 12000` in a different repo
- Catalog team: no throttle at all — "we are read-heavy, we are fine"

Three divergent limits, zero coordination. The incident commander needs **one global RPM cap** propagated to every enforcement point before origin pools saturate.

> "Throttle during an incident is **operational**. Why are we editing three repos while the graph goes vertical?"

[Kiponos.io](https://kiponos.io) holds incident throttle trees under `['incident']['prod']['throttle']` — one hub profile, local `getInt()` on every request, `afterValueChanged` to resize token buckets fleet-wide without redeploy.

## The problem — global_rpm_cap baked into static config

Your API gateway enforces a frozen constant:

```java
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int GLOBAL_RPM = 8000;
    private final RateLimiter globalLimiter = RateLimiter.create(GLOBAL_RPM / 60.0);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        if (!globalLimiter.tryAcquire()) {
            res.setStatus(429);
            return;
        }
        chain.doFilter(req, res);
    }
}
```

Spring YAML duplicates the number elsewhere:

```yaml
incident:
  throttle:
    global_rpm_cap: 8000
    burst_allowance: 400
```

During a DDoS spike, ops needs `global_rpm_cap: 3000` **in one edit** across gateway, search, and catalog pods — not three PRs and a staggered rollout while attackers adapt.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Edge WAF handles DDoS — origin throttles are backup" | WAF rule lag leaves origin exposed for minutes |
| "Each service knows its own capacity" | Uncoordinated limits let one service absorb the flood |
| "Scale horizontally instead of throttling" | Attack volume scales faster than your autoscaler |
| "Global RPM in gateway is enough" | Internal service-to-service paths bypass the gateway |
| "We will lower limits in the next deploy" | Origin CPU hits 100% before CI finishes |

## The Aha

**global_rpm_cap is operational config** — it changes during DDoS spikes, abuse waves, and capacity emergencies. It belongs in a **shared live tree** every enforcing service reads with `getInt()`, not in per-repo constants that diverge under stress.

## What Kiponos.io is for incident throttles

Profile `['incident']['prod']['throttle']` is the **single coordination point** for gateway, search, and catalog JVMs. Dashboard edit on `throttle/global/global_rpm_cap` sends a **delta** to every connected SDK.

`kiponos.path("throttle", "global").getInt("global_rpm_cap")` is a **local memory read** in the filter hot path — no Redis round-trip per request, no polling a central config API.

`afterValueChanged` calls `setRate()` on the live `RateLimiter` (or Resilience4j `RateLimiter` registry entry) when ops moves the cap — same JVM keeps serving, new throughput ceiling applies immediately.

Honest boundary: Kiponos does **not** replace CDN or WAF DDoS protection, per-tenant billing limits, or long-term capacity planning. It owns **incident-coordinated RPM floats** your Java services enforce locally.

## Architecture

![Architecture diagram](https://files.catbox.moe/cl0x8k.png)

## Config tree

```yaml
throttle/
  global/
    global_rpm_cap: 8000
    burst_allowance: 400
    enabled: true
  per_service/
    search/
      rpm_cap: 12000
      inherit_global: true
    catalog/
      rpm_cap: 6000
      inherit_global: true
    checkout/
      rpm_cap: 4000
      inherit_global: false
  incident/
    ddos_mode: false
    ddos_global_rpm_cap: 3000
    ddos_burst_allowance: 50
  ops/
    owner: incident-commander
    last_flip_by: ""
    notes: "Lower global cap before per-service tweaks"
```

## Integration (Spring Boot 3 + Resilience4j RateLimiter)

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
public class IncidentThrottleFilter extends OncePerRequestFilter {

    private final Kiponos kiponos;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final MeterRegistry metrics;
    private volatile RateLimiter globalLimiter;

    public IncidentThrottleFilter(Kiponos kiponos, RateLimiterRegistry rateLimiterRegistry,
                                  MeterRegistry metrics) {
        this.kiponos = kiponos;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.metrics = metrics;
        kiponos.afterValueChanged(this::onThrottleChange);
        globalLimiter = rebuildGlobalLimiter();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        var global = kiponos.path("throttle", "global");
        if (!global.getBool("enabled", true)) {
            chain.doFilter(req, res);
            return;
        }
        if (!globalLimiter.acquirePermission()) {
            res.setStatus(429);
            res.setHeader("Retry-After", "1");
            metrics.counter("incident.throttle.rejected").increment();
            return;
        }
        chain.doFilter(req, res);
    }

    private void onThrottleChange(ValueChange change) {
        if (change.path().startsWith("throttle/")) {
            globalLimiter = rebuildGlobalLimiter();
            log.warn("Incident throttle resized: global_rpm_cap={}", resolveGlobalRpmCap());
            metrics.counter("incident.throttle.resized").increment();
        }
    }

    private RateLimiter rebuildGlobalLimiter() {
        int rpm = resolveGlobalRpmCap();
        int burst = resolveBurstAllowance();
        return rateLimiterRegistry.rateLimiter("incident-global", RateLimiterConfig.custom()
                .limitForPeriod(rpm)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build());
    }

    private int resolveGlobalRpmCap() {
        if (kiponos.path("throttle", "incident").getBool("ddos_mode", false)) {
            return kiponos.path("throttle", "incident")
                    .getInt("ddos_global_rpm_cap", 3000);
        }
        return kiponos.path("throttle", "global").getInt("global_rpm_cap", 8000);
    }

    private int resolveBurstAllowance() {
        if (kiponos.path("throttle", "incident").getBool("ddos_mode", false)) {
            return kiponos.path("throttle", "incident")
                    .getInt("ddos_burst_allowance", 50);
        }
        return kiponos.path("throttle", "global").getInt("burst_allowance", 400);
    }
}
```

Ops enables `ddos_mode` and sets `ddos_global_rpm_cap: 3000`. Gateway, search, and catalog pods — all on the same profile — resize limits within seconds.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| DDoS spike — ops lowers global RPM in one edit | Three PRs, divergent caps | `ddos_mode` + `global_rpm_cap` live across fleet |
| Attack subsides | Manual revert per service | Disable `ddos_mode`; restore 8000 in one click |
| Partial path flood — search only | Gateway throttle too blunt | `per_service/search/rpm_cap` override |
| Load test validation | Branch per RPM value | Hub profile `staging/throttle` |
| Post-incident review | Slack screenshots of three configs | Hub audit: who set 3000 at 16:51 |

## Performance on the gateway hot path

- **`acquirePermission()` local** — Resilience4j in-process; `getInt()` is memory read before check
- **Limiter rebuild on change only** — not per HTTP request
- **One WebSocket per pod** — coordinated tree without Redis pub/sub per request
- **Delta patch** — `global_rpm_cap` 8000 → 3000 is one integer to entire fleet
- **Shared profile** — gateway/search/catalog enforce same cap without shared Redis state

## Compare to alternatives

| Approach | One-edit fleet-wide RPM cap | Hot-path read cost | Cross-service coordination |
|----------|----------------------------|-------------------|---------------------------|
| Per-service YAML constants | N deploys | Zero (frozen) | None |
| Redis global counter | Yes | RTT per request | Central but slow |
| API gateway alone | Single point | N/A at edge | Internal paths bypass |
| Spring Cloud Config refresh | Staggered recycle | Network on refresh | Eventual consistency |
| **Kiponos live hub** | **Seconds, one tree** | **Local get*()** | **Same profile all JVMs** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| CDN / WAF DDoS mitigation | Cloudflare, AWS Shield, edge rules |
| Per-tenant billing rate limits | Billing service + contract tables |
| Long-term capacity planning | Architecture review + load tests |
| Geographic blocking by country | Edge policy or GeoIP rules |
| Bootstrap Resilience4j instance wiring | Git-reviewed YAML is fine |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['incident']['prod']['throttle']`.
3. Add `io.kiponos:sdk-boot-3` and Resilience4j to gateway, search, and catalog services.
4. Point all three at `-Dkiponos="['incident']['prod']['throttle']"`.
5. Move `global_rpm_cap` out of per-service constants into `throttle/global/`.
6. Wire `IncidentThrottleFilter` with `afterValueChanged` limiter rebuild.
7. Staging game day: flood search endpoint, enable `ddos_mode`, confirm 429 rate climbs **without pod restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Rate limits and circuit breakers](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-rate-limits-circuit-breakers.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*global_rpm_cap belongs in the live ops tree — not in three repos that diverge while the attack adapts.*