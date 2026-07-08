---
title: "Multi-Tenant Quota Trees — Per-Customer Limits Ops Can Tune Live (Java SDK)"
published: false
tags: java, saas, finops, architecture
description: Per-tenant limits trapped in SQL seed data require migrations. Kiponos nests quota knobs per tenant with local reads on the entitlement hot path.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-multi-tenant-quota-trees.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

Wednesday 08:41 UTC. PagerDuty fires **API latency SLO burn** on **saas-gateway** — p99 doubled in ten minutes. Traces show **92%** of CPU on one API key prefix: tenant **Acme Robotics** (`tenant_acme`) running a misconfigured ETL loop at **4,800 req/min** against a plan that allows **1,200**.

Customer success manager **Riley Chen** is on the bridge with platform SRE **Omar Haddad**. Riley cannot suspend the contract — Acme is mid-migration. Omar needs to **throttle Acme only** without touching **Globex Analytics** or the other 340 tenants on the shared cluster:

> "Set `requests_per_minute` for `tenant_acme` to **400** now. Leave the default tier at 1200. I am not running a SQL migration while the gateway is melting."

The entitlement service still reads quotas from a `HashMap` seeded at boot:

```java
tenantQuota.put("tenant_acme", 1200);
```

The billing database has the "correct" plan limits, but changing them requires a migration script, cache invalidation, and a **rolling restart** of six gateway pods. Redis rate counters are live; the **ceilings** are not.

The VP of Platform asks:

> "We already resolve tenant on every request. Why does **noisy-neighbor isolation** require a **deploy** when the knob is an integer per tenant?"

Most Java SaaS gateways treat per-tenant quotas as **bootstrap data**: SQL seed rows, a static map in code, and support tickets that take hours while neighbors suffer. [Kiponos.io](https://kiponos.io) collapses per-tenant `requests_per_minute`, tier defaults, and emergency throttle flags into **one operational tree** — readable on every API request with local `get*()` calls and adjustable from the dashboard while JVMs keep running.

## The problem — requests_per_minute baked into static config

A typical multi-tenant gateway enforces quotas like this:

```java
@Service
public class TenantRateLimiter {
    private static final Map<String, Integer> TENANT_RPM = Map.of(
        "tenant_acme", 1200,
        "tenant_globex", 2400,
        "default", 600
    );

    public boolean allow(String tenantId) {
        int limit = TENANT_RPM.getOrDefault(tenantId, TENANT_RPM.get("default"));
        return tokenBucket.tryConsume(tenantId, limit);
    }
}
```

Quota policy usually lives elsewhere — scattered and deploy-bound:

```yaml
# entitlements-seed.sql — requires migration + pod recycle
INSERT INTO tenant_limits (tenant_id, requests_per_minute) VALUES
  ('tenant_acme', 1200),
  ('tenant_globex', 2400);
```

Or worse — one global limit because per-tenant config got messy:

```java
private static final int REQUESTS_PER_MINUTE = 600;  // "fair" — until one tenant isn't
```

The gateway path executes **thousands of entitlement checks per second**. During a noisy-neighbor incident you need to:

1. Lower **`tenants/tenant_acme/requests_per_minute`** without touching other tenants
2. Keep **`tiers/enterprise/requests_per_minute`** as the default for well-behaved enterprise accounts
3. Flip **`posture/noisy_neighbor_mode`** to apply stricter burst multipliers cluster-wide

Doing that through a deploy while p99 burns is not SaaS operations — it is **noisy-neighbor theater with churn risk**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Billing DB is source of truth for limits" | Gateway hot path reads boot-time map — DB changes lag hours |
| "API gateway rate limits are per-route" | Per-tenant fairness needs **per-tenant ceilings** on shared routes |
| "We'll upgrade Acme's plan" | Contract workflow takes days; neighbors need relief in **minutes** |
| "Redis token bucket is enough" | Redis stores consumption; **ceilings** still live in stale constants |
| "Noisy neighbor means bad architecture" | Noisy neighbors happen — ops needs a **surgical throttle**, not a sermon |

## The Aha

**requests_per_minute is operational config** — it changes during noisy-neighbor incidents, plan migrations, and FinOps anomalies. It belongs in a **live tree** the gateway already reads with `getInt()`, not in a `HashMap` populated at JVM boot.

## What Kiponos.io is for multi-tenant quota trees

[Kiponos.io](https://kiponos.io) is a real-time configuration hub with Java and Python SDKs. `Kiponos.createForCurrentTeam()` connects over WebSocket; the profile tree — for example `['saas']['prod']['quotas']` — hydrates into **in-process memory** at service startup.

When Omar sets `tenants/tenant_acme/requests_per_minute` to `400`, a **delta** patches only that key. The next `kiponos.path("tenants", tenantId).getInt("requests_per_minute")` on an incoming API call is a **local memory read** — no HTTP to a config API, no JDBC poll on the hot path, no extra Redis round-trip for policy.

`afterValueChanged` logs quota flips and can emit `tenant_quota_changed` metrics to your support dashboard **without** restarting gateway pods.

No restart. No redeploy. No `@RefreshScope` bean recycle.

Honest boundary: Kiponos does **not** replace your billing system for invoices, CRM for contract entitlements, or identity for API key issuance. It owns **runtime throttle ceilings** Java gateways read on every request.

## Architecture

![Architecture diagram](https://files.catbox.moe/cuhhiv.png)

**CRM documents contract entitlements; authoritative runtime ceilings live in Kiponos** where throttling one tenant takes seconds.

## Config tree — tiers, tenants, burst, posture, and audit

Five folders — `defaults`, `tiers`, `tenants`, `posture`, `audit`:

```yaml
defaults/
  requests_per_minute: 600
  fallback_tier: free
  enforce_per_tenant_override: true
tiers/
  free/
    requests_per_minute: 120
    burst_multiplier: 1.0
  pro/
    requests_per_minute: 600
    burst_multiplier: 1.2
  enterprise/
    requests_per_minute: 2400
    burst_multiplier: 1.5
tenants/
  tenant_acme/
    requests_per_minute: 1200
    tier: enterprise
    throttle_override: false
  tenant_globex/
    requests_per_minute: 2400
    tier: enterprise
    throttle_override: false
  tenant_startup42/
    requests_per_minute: 300
    tier: pro
    throttle_override: false
posture/
  noisy_neighbor_mode: false
  noisy_neighbor_burst_multiplier: 0.5
  return_retry_after_sec: 10
audit/
  last_quota_change_by: ""
  last_quota_change_at_ms: 0
  emit_429_metrics: true
```

One tree. One profile path: `['saas']['prod']['quotas']`. Staging noisy-neighbor drills share **identical key layout** — only values differ.

## Java integration — per-tenant quota resolver + noisy-neighbor posture

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
public class KiponosQuotaConfig {
    @Bean
    public Kiponos kiponos() {
        Kiponos client = Kiponos.createForCurrentTeam();
        // Profile: ['saas']['prod']['quotas'] via -Dkiponos=... JVM arg
        client.afterValueChanged(change ->
            log.info("Quota delta: path={} value={}", change.path(), change.newValue())
        );
        return client;
    }
}

@Service
public class TenantQuotaService {
    private final Kiponos kiponos;
    private final TokenBucketRegistry buckets;

    public int resolveRequestsPerMinute(String tenantId) {
        var defaults = kiponos.path("defaults");
        int base = defaults.getInt("requests_per_minute", 600);

        var tenant = kiponos.path("tenants", tenantId);
        if (tenant.exists()) {
            String tier = tenant.get("tier", defaults.get("fallback_tier", "free"));
            base = kiponos.path("tiers", tier).getInt("requests_per_minute", base);

            if (defaults.getBool("enforce_per_tenant_override", true)) {
                base = tenant.getInt("requests_per_minute", base);
            }
        }

        var posture = kiponos.path("posture");
        if (posture.getBool("noisy_neighbor_mode", false)
            && tenant.exists() && tenant.getBool("throttle_override", false)) {
            double mult = posture.getDouble("noisy_neighbor_burst_multiplier", 0.5);
            base = (int) (base * mult);
        }

        return Math.max(base, 1);
    }

    public RateLimitResult check(String tenantId) {
        int rpm = resolveRequestsPerMinute(tenantId);
        if (!buckets.tryConsume(tenantId, rpm)) {
            if (kiponos.path("audit").getBool("emit_429_metrics", true)) {
                metrics.inc("tenant_quota_denied", tenantId);
            }
            int retry = kiponos.path("posture").getInt("return_retry_after_sec", 10);
            return RateLimitResult.denied(retry);
        }
        return RateLimitResult.allowed();
    }
}

@Service
public class ApiGatewayFilter {
    private final TenantQuotaService quotas;

    public void preHandle(HttpServletRequest req) {
        String tenantId = resolveTenant(req);
        RateLimitResult result = quotas.check(tenantId);
        if (result.denied()) {
            throw new TooManyRequestsException(result.retryAfterSec());
        }
    }
}
```

Every `getInt()`, `getBool()`, and `get()` on the entitlement path is **O(1) local cache** — microseconds, not cross-region config service RTT.

**Usage counters** stay in Redis or your metering pipeline — Kiponos owns the **ceilings** that change when support rings the alarm.

## Real scenarios

| Event | Without Kiponos | With Kiponos |
|-------|-----------------|--------------|
| Noisy neighbor — throttle one tenant without affecting others | SQL migration + cache flush + rolling restart | Dashboard: `tenants/tenant_acme/requests_per_minute: 400` live |
| Enterprise migration window — temporary cap | Support ticket + eng queue | Set per-tenant override; revert after migration |
| FinOps wants lower default for new signups | Deploy new `default` constant | `defaults/requests_per_minute` live — existing tenants unchanged |
| Abuse spike on free tier | Global throttle hurts paying tenants | `tiers/free/requests_per_minute` only |
| Post-incident restore | Second deploy to reset map | Reset tenant subtree in one edit |

## Performance — hot path on multi-tenant entitlement checks

- **Quota resolution per request** — four local reads (defaults, tenant, tier, posture); no JDBC on API path
- **Per-tenant nesting** — hundreds of tenants as folders; no `TENANT_ACME_RPM` env var explosion
- **Delta updates** — throttling one tenant sends one patch; neighbors never reload config
- **Token bucket unchanged** — Redis still tracks consumption; only ceilings move live
- **One WebSocket per JVM** — background sync; gateway never blocks on config API RTT
- **Complements billing DB** — contracts stay in CRM; gateway owns **runtime throttle authority**

## Compare to alternatives

| Approach | Surgical tenant throttle | Hot-path read latency | Tier + tenant nesting |
|----------|-------------------------|----------------------|------------------------|
| SQL seed + migration | Poor — hours | Zero after restart but stale | Possible — DB round-trip if polled |
| Static HashMap at boot | Poor — redeploy | Zero (static) but stale | Awkward — code change per tenant |
| Redis hash of limits | Medium — mixed with usage keys | Sub-ms but namespace collision | Flat keys sprawl |
| API management SaaS | Good — external console | Extra network hop per request | Vendor schema |
| Feature-flag SaaS | Booleans only | Network evaluation | Poor for per-tenant integers |
| **Kiponos live hub** | **Seconds — one tenant key** | **Local get*()** | **First-class folder tree** |

## When not to use Kiponos

| Case | Use instead |
|------|-------------|
| Contract PDFs and legal entitlement documents | CRM / legal workflow |
| Invoice generation and usage billing | Billing warehouse + metering pipeline |
| API key creation and rotation | Identity provider / secret manager |
| Immutable usage records for audit | Metering store — source of truth |
| One-time bootstrap quotas from sales onboarding | SQL seed at tenant provision time |

## Getting started (15 minutes)

1. Sign up at [kiponos.io](https://kiponos.io) (TeamPro).
2. Create profile path `['saas']['prod']['quotas']`.
3. Add `tiers/enterprise/requests_per_minute`, `tenants/tenant_acme/requests_per_minute`, and wire `resolveRequestsPerMinute()` in your gateway filter.
4. `./gradlew bootRun` — confirm log shows WebSocket handshake.
5. Lower `tenants/tenant_acme/requests_per_minute` in dashboard; confirm 429 rate rises for Acme **without** JVM restart.
6. Drill: enable `posture/noisy_neighbor_mode` for a flagged tenant in staging.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Inference spend caps](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-finops-inference-spend-caps.md)
- [LLM token budget per user](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-llm-token-budget-per-user.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*requests_per_minute belongs in the live ops tree — not in a HashMap that mocks your neighbors during the next noisy-tenant incident.*