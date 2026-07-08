---
title: "Degradation Mode Switches — Flip Read-Only Posture Live (Java SDK)"
published: false
tags: java, sre, architecture, devops
description: Feature toggles for degradation mixed with product flags. Kiponos holds ops-owned degradation trees separate from experiments.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-reliability-degradation-mode-switches.md
main_image: https://files.catbox.moe/87bzmo.jpg
---

Sunday 11:03 UTC. Primary Postgres on the catalog API hits **98% connection utilization**. Replication lag on the read replica crosses four minutes. Writes still flow — every new review and inventory adjustment hammers the primary while reads could survive on stale replicas.

The DBA posts in the bridge: "Flip to read-only on write paths — give the primary room to breathe."

Someone opens the feature-flag console. `read_only_mode` sits beside `new_checkout_ui` and `summer_promo_banner` — product experiments mixed with **incident posture**. Flipping it requires finding the right flag, worrying about cohort bleed, and hoping the SDK cache refreshes before more writes land.

The platform engineer mutters:

```java
private static final boolean READ_ONLY_MODE = false;
```

> "Degradation is **operational**. Why is it sharing a console with A/B tests?"

[Kiponos.io](https://kiponos.io) holds ops-owned degradation trees under `['api']['prod']['degradation']` — separate from product flags, local `getBool()` on every mutating request, `afterValueChanged` to flip filters live.

## The problem — read_only_mode baked into static config

Your catalog API guards writes with a constant or env var:

```java
@RestController
public class ReviewController {

    @PostMapping("/api/v1/reviews")
    public ResponseEntity<Review> createReview(@RequestBody ReviewRequest req) {
        if (READ_ONLY_MODE) {
            return ResponseEntity.status(503)
                    .header("Retry-After", "300")
                    .build();
        }
        return ResponseEntity.ok(reviewService.create(req));
    }
}
```

Spring YAML mirrors the constant:

```yaml
api:
  degradation:
    read_only_mode: false
    maintenance_message: "Catalog temporarily read-only"
```

During database pressure, you need `read_only_mode: true` **instantly** — not after a deploy, not after a feature-flag poll cycle, not after `@RefreshScope` recycles controllers under peak read traffic.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Feature flags can handle degradation" | Product flags lack ops audit and incident semantics |
| "Read-only is a deploy-time maintenance window" | DB pressure arrives unscheduled — posture must flip in seconds |
| "We will block writes at the load balancer" | LB rules are coarse — cannot exempt internal admin paths |
| "Env var + rolling restart is fast enough" | Restart under DB pressure risks connection stampede on boot |
| "Degradation belongs in runbook wiki" | Runbooks describe **when**; the switch must be **one click** |

## The Aha

**read_only_mode is operational config** — it changes during database pressure, failover drills, and schema migrations. It belongs in a **live ops tree** your API reads with `getBool()` on every mutating route, not in a constant compiled into the controller.

## What Kiponos.io is for degradation switches

Profile `['api']['prod']['degradation']` syncs `read_only_mode`, exempt paths, and user-facing messages into every API pod. Dashboard flip sends a **delta**; the next `POST` hits the new posture via local read.

`kiponos.path("degradation", "write_guard").getBool("read_only_mode")` is a **local memory read** in the servlet filter — zero network on the request hot path.

`afterValueChanged` updates the filter's cached posture and logs the actor for post-incident review — no controller bean recycle.

Honest boundary: Kiponos does **not** replace database failover automation, connection pool tuning (see Hikari live articles), or product experiment platforms. It owns **ops degradation booleans** your Java API enforces per request.

## Architecture

![Architecture diagram](https://files.catbox.moe/tfawum.png)

## Config tree

```yaml
degradation/
  write_guard/
    read_only_mode: false
    retry_after_seconds: 300
    enabled: true
  messages/
    public_body: "Catalog temporarily read-only — try again shortly"
    internal_body: "DB pressure — write guard active"
  exemptions/
    admin_paths: /internal/ops,/health
    service_accounts: ops-bot,schema-migrator
  paths/
    block_writes_on: /api/v1/reviews,/api/v1/inventory
    allow_reads_on: /api/v1/catalog,/api/v1/search
  ops/
    owner: platform-dba
    auto_flip_on_db_pressure: false
```

## Integration (Spring Boot 3)

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
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DegradationWriteFilter extends OncePerRequestFilter {

    private final Kiponos kiponos;
    private final MeterRegistry metrics;
    private volatile DegradationPosture posture;

    public DegradationWriteFilter(Kiponos kiponos, MeterRegistry metrics) {
        this.kiponos = kiponos;
        this.metrics = metrics;
        kiponos.afterValueChanged(this::onDegradationChange);
        posture = loadPosture();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        if (isMutating(req.getMethod()) && posture.readOnly && !isExempt(req)) {
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.setHeader("Retry-After", String.valueOf(posture.retryAfterSeconds));
            res.getWriter().write(posture.publicMessage);
            metrics.counter("degradation.write_blocked").increment();
            return;
        }
        chain.doFilter(req, res);
    }

    private void onDegradationChange(ValueChange change) {
        if (change.path().startsWith("degradation/")) {
            posture = loadPosture();
            log.warn("Degradation posture updated: read_only_mode={}", posture.readOnly);
            metrics.counter("degradation.posture_flip").increment();
        }
    }

    private DegradationPosture loadPosture() {
        var guard = kiponos.path("degradation", "write_guard");
        var messages = kiponos.path("degradation", "messages");
        return new DegradationPosture(
                guard.getBool("read_only_mode", false),
                guard.getInt("retry_after_seconds", 300),
                messages.getString("public_body",
                        "Catalog temporarily read-only — try again shortly"));
    }

    private boolean isExempt(HttpServletRequest req) {
        String path = req.getRequestURI();
        return kiponos.path("degradation", "exemptions")
                .getString("admin_paths", "")
                .contains(path);
    }

    private boolean isMutating(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)
                || "DELETE".equals(method);
    }

    private record DegradationPosture(boolean readOnly, int retryAfterSeconds,
                                      String publicMessage) {}
}
```

Ops sets `read_only_mode: true` during connection pressure. The filter blocks mutating routes on the **next request** — primary gets breathing room, reads continue on replicas.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Database pressure — enable read-only on write paths | PR + rolling restart | Dashboard flip; filter enforces on next POST |
| Failover drill | Maintenance window + deploy | `read_only_mode` live; exempt migrator paths |
| Recovery — primary healthy | Second deploy | `read_only_mode: false`; audit in hub |
| Schema migration window | Mixed product flags | Ops tree separate from `new_checkout_ui` |
| Partial degradation | Block reviews only | `paths/block_writes_on` scoped keys |

## Performance on the request hot path

- **`getBool()` inside filter** — local read; runs once per mutating request, not per field
- **`volatile` posture cache** — filter reads cached struct; hub merge happens async on WebSocket thread
- **`afterValueChanged` on flip** — posture reload once per dashboard edit, not per HTTP call
- **One WebSocket per API pod** — not Redis poll before every POST
- **Exempt path check** — local string match from tree; no DB lookup for admin routes

## Compare to alternatives

| Approach | Flip read-only during DB pressure | Hot-path read cost | Ops vs product separation |
|----------|----------------------------------|-------------------|--------------------------|
| Static YAML + deploy | 20+ minutes | N/A until restart | N/A |
| Product feature-flag SaaS | Fast but mixed cohorts | Network RTT | Poor |
| `@RefreshScope` controllers | Context refresh | Bean recycle risk | Awkward |
| Load balancer maintenance mode | Coarse — all or nothing | N/A | No per-path |
| **Kiponos live hub** | **Seconds** | **Local get*()** | **Dedicated ops tree** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Automatic Postgres failover (Patroni, RDS) | Infrastructure automation |
| Connection pool max size tuning | Hikari live binder + DBA runbook |
| Product A/B experiments and cohort targeting | Experimentation platform |
| Full API maintenance page branding | CMS or static assets |
| Bootstrap filter registration order | Git-reviewed Spring config |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['api']['prod']['degradation']`.
3. Add `io.kiponos:sdk-boot-3` to catalog API service.
4. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['api']['prod']['degradation']"`.
5. Move `read_only_mode` out of YAML and product flags into `degradation/write_guard/`.
6. Register `DegradationWriteFilter` with `afterValueChanged` posture reload.
7. Staging game day: simulate DB pressure, flip `read_only_mode`, confirm POST returns 503 while GET succeeds **without pod restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Black Friday runbook live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-black-friday-runbook-live.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*read_only_mode belongs in the live ops tree — not in constants sharing a console with summer promos.*