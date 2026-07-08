---
title: "Audit Log Sampling Rates Live — Tune Capture Intensity Without Recycling Pods (Java SDK)"
published: false
tags: java, compliance, architecture, devops
description: HIPAA-style capture rates in properties files are not incident knobs. Kiponos adjusts sampling — operational tuning, not compliance certification.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-audit-log-sampling-rates.md
main_image: https://files.catbox.moe/574r3o.jpg
---

Wednesday 03:41 UTC. Elasticsearch ingest crosses **92% of daily quota** on the health-audit cluster. The on-call SRE drops `sample_rate_pct` from 15 to 4 in the runbook spreadsheet — then realizes the JVM still reads `audit.sample.rate=0.15` from `application-health-prod.yml` baked in fourteen months ago when legal signed off on a cost model, not a clinical incident.

Forty-two Spring Boot audit emitters across three AZs keep sampling at 15%. Storage alarms escalate. The compliance liaison pings:

> "We are **not** asking for a HIPAA attestation change — we need **ops** to throttle capture until the index rollover finishes. Why does that require a **pod recycle**?"

**Honest framing:** this article is about **operational tuning agility** for audit capture intensity. Kiponos lets platform ops move `sample_rate_pct` while services run. It does **not** replace your compliance packet, BAA language, retention legal holds, or QSA evidence program. The tree holds **how hard you sample right now** — not whether you are certified.

Most Java health and fintech services encode audit sampling as **three artifacts**: a compliance PDF, static YAML tuned for storage cost, and `private static final double SAMPLE_RATE` in the audit filter. [Kiponos.io](https://kiponos.io) collapses the **runtime knobs** into one profile tree — default sample percent, endpoint overrides, and investigation-mode flags — readable on every emit path with **local `get*()` calls**.

## The problem: sample_rate_pct frozen on the emit hot path

A typical audit interceptor still samples from boot-time constants:

```java
@Component
public class AuditSampleFilter implements Filter {
    private static final double SAMPLE_RATE_PCT = 15.0;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (ThreadLocalRandom.current().nextDouble(100) < SAMPLE_RATE_PCT) {
            auditBus.emit(buildEvent((HttpServletRequest) req));
        }
        chain.doFilter(req, res);
    }
}
```

Ops policy lives elsewhere — static and restart-bound:

```yaml
# application-health-prod.yml — rolling restart required
health:
  prod:
    audit:
      sample_rate_pct: 15
      endpoint_overrides: {}
```

Or worse — a FinOps ticket closed the rate at 15% with no runtime escape hatch:

```properties
audit.sample.rate=0.15
```

During storage pressure you need to:

1. Lower **`sample_rate_pct`** to 4 until rollover completes
2. Keep **`endpoints.critical.sample_rate_pct`** at 100 for break-glass APIs
3. Flip **`investigation_mode`** so security can temporarily raise fidelity on one service class

Doing that through Helm while ingest alarms fire is **ops theater** — the constant still gates `shouldSample()` on every request.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Sampling rate is a compliance constant" | Storage incidents need hourly throttle — not quarterly CAB |
| "We'll scale Elasticsearch" | Replicas do not change JVM `SAMPLE_RATE_PCT` without restart |
| "Audit policy lives in the wiki" | Wiki pages do not gate `nextDouble()` on the hot path |
| "Investigation mode is a SIEM rule" | Rules cannot recover events the JVM never emitted |
| "Staging mirrors prod sampling" | Endpoint override keys never seeded in lower envs |

## The Aha

**`sample_rate_pct` is operational config** — it shifts during storage incidents, cost anomalies, and security investigations. It belongs in a **live tree** the service reads with `kiponos.path("audit").getFloat("sample_rate_pct")`, not in a `static final` imported at boot.

## What Kiponos.io is for audit sampling (RegOps)

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Each Spring Boot service connects **once** at startup over WebSocket; profile `['health']['prod']['audit']` hydrates an **in-memory cache** in the Java SDK.

When ops sets `sample_rate_pct` to `4`, a **delta** patches only that key. The next `getFloat("sample_rate_pct")` on an audit decision is a **local memory read** — no HTTP config fetch, no Redis poll on the request path.

`afterValueChanged` listeners log who moved sampling policy, forward meta-audit events to SIEM, and increment `audit_sample_policy_change_total` **without** recycling pods.

No restart. No redeploy. No `@RefreshScope` bean recycle.

**RegOps boundary:** Kiponos records **who changed operational capture intensity** via dashboard ACL — useful evidence alongside your compliance program. It does **not** certify HIPAA, PCI, or SOC2 posture by itself.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/un2ptp.png)

## Config tree — sampling, endpoints, investigation, storage guards

Five folders — `audit`, `endpoints`, `investigation`, `storage`, `meta`:

```yaml
audit/
  sample_rate_pct: 15.0
  min_sample_rate_pct: 1.0
  max_sample_rate_pct: 100.0
  emit_async: true
  queue_drop_policy: oldest_first
endpoints/
  critical/
    sample_rate_pct: 100.0
    path_prefixes: ["/api/v1/break-glass", "/api/v1/admin"]
  read_heavy/
    sample_rate_pct: 8.0
    path_prefixes: ["/api/v1/patient", "/api/v1/chart"]
investigation/
  mode_enabled: false
  boost_sample_rate_pct: 100.0
  expires_at_ms: 0
  target_service_ids: []
storage/
  throttle_on_quota_pct: 90
  throttle_sample_rate_pct: 4.0
  auto_restore_when_quota_below_pct: 75
meta/
  last_change_by: ""
  last_change_at_ms: 0
  siem_forward_enabled: true
```

One profile path: `['health']['prod']['audit']`. Staging storage drills use **identical key layout** — only values differ.

## Java integration: live audit sampling + afterValueChanged

```java
import io.kiponos.sdk.Kiponos;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RegOpsLiveAuditFilter implements Filter {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final AuditEmitter auditEmitter;

    public RegOpsLiveAuditFilter(AuditEmitter auditEmitter) {
        this.auditEmitter = auditEmitter;
        kiponos.afterValueChanged(change -> {
            log.info("Audit sampling delta: path={} value={}", change.path(), change.newValue());
            if (kiponos.path("meta").getBool("siem_forward_enabled")) {
                siemClient.emit("regops_audit_sample_change", change.path(), change.newValue());
            }
            metrics.counter("audit_sample_policy_change").inc();
        });
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        if (shouldCapture(http)) {
            auditEmitter.emitAsync(buildEvent(http));
        }
        chain.doFilter(req, res);
    }

    private boolean shouldCapture(HttpServletRequest http) {
        String path = http.getRequestURI();

        if (kiponos.path("endpoints", "critical").getList("path_prefixes").stream()
                .anyMatch(path::startsWith)) {
            return ThreadLocalRandom.current().nextDouble(100)
                < kiponos.path("endpoints", "critical").getFloat("sample_rate_pct");
        }

        var investigation = kiponos.path("investigation");
        if (investigation.getBool("mode_enabled")
            && System.currentTimeMillis() < investigation.getLong("expires_at_ms")) {
            return ThreadLocalRandom.current().nextDouble(100)
                < investigation.getFloat("boost_sample_rate_pct");
        }

        float rate = resolveEffectiveSampleRate(path);
        return ThreadLocalRandom.current().nextDouble(100) < rate;
    }

    private float resolveEffectiveSampleRate(String path) {
        var storage = kiponos.path("storage");
        if (esQuotaMonitor.currentQuotaPct() >= storage.getInt("throttle_on_quota_pct")) {
            return storage.getFloat("throttle_sample_rate_pct");
        }
        if (kiponos.path("endpoints", "read_heavy").getList("path_prefixes").stream()
                .anyMatch(path::startsWith)) {
            return kiponos.path("endpoints", "read_heavy").getFloat("sample_rate_pct");
        }
        return kiponos.path("audit").getFloat("sample_rate_pct");
    }
}
```

Every `getFloat()` and `getBool()` on the sampling path is **O(1) local cache** — microseconds, not cross-region config RTT.

## Real-world scenarios

| Scenario | Without live audit tree | With Kiponos RegOps sampling |
|----------|-------------------------|------------------------------|
| ES quota at 92% | Helm restart; clinical API blips | `storage/throttle_sample_rate_pct: 4` live |
| Security investigation on one API class | Second deploy to raise fidelity | `investigation/mode_enabled: true` + expiry |
| Post-rollover restore | Another PR wave | Reset `sample_rate_pct` in dashboard |
| Critical break-glass endpoints | Same global 4% rate | `endpoints/critical` stays at 100% |
| Auditor asks who throttled capture | Spreadsheet + deploy tickets | Kiponos ACL + `afterValueChanged` SIEM stream |

## Performance: why sampling gates must stay local

- **One WebSocket per JVM** — not one config fetch per audited request
- **Effective rate resolution is ≤5 local reads** — nanoseconds vs audit bus I/O
- **Delta patches** — lowering global sample rate sends one patch, not full tree reload
- **`emit_async: true`** — policy reads stay off the critical servlet chain latency
- **No YAML re-parse** on every request during storage incidents

In load tests, Kiponos reads are noise; Elasticsearch bulk indexing and app logic dominate.

## Compare to alternatives

| Approach | Mid-incident sample throttle | Hot-path read latency | Endpoint overrides + investigation mode |
|----------|------------------------------|----------------------|----------------------------------------|
| application.yml + restart | No — pod recycle | Zero (static) but stale | Partial — restart per wave |
| SIEM-side sampling only | Post-hoc — misses unsampled events | N/A at JVM | No — source never emitted |
| Database audit policy | Yes with JDBC | Milliseconds per request | Possible — schema discipline |
| Redis config hash | Yes with poll | Poll jitter on hot path | Custom key schema |
| Feature-flag SaaS | Booleans awkward for floats | SDK network per eval | Poor for nested endpoint trees |
| **Kiponos SDK** | **Yes — dashboard delta + ACL** | **Zero (in-process cache)** | **Yes — one profile tree** |

## When not to use Kiponos for audit sampling

| Boundary | Better home |
|----------|-------------|
| Legal retention periods and immutable WORM storage | SIEM / Object Lock architecture |
| PHI encryption keys, TLS certs, BAA contracts | HSM / Vault / legal docs |
| Whether 10% sampling satisfies your compliance program | Compliance officer + counsel — not this article |
| The captured audit events themselves | Elasticsearch / OpenSearch |
| Network segmentation for regulated VLANs | Infrastructure compliance |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — add profile `['health']['prod']['audit']` with `audit`, `endpoints`, `investigation`, and `storage` folders. Restrict dashboard ACL to platform + security ops.
2. Add `io.kiponos:sdk-boot-3` to your Spring Boot service.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['health']['prod']['audit']"`.
4. Replace `SAMPLE_RATE_PCT` constants with `kiponos.path("audit").getFloat("sample_rate_pct")` and endpoint overrides.
5. Register `RegOpsLiveAuditFilter` and wire `afterValueChanged` SIEM forwarding.
6. Drill: in staging, simulate ES quota pressure and lower `sample_rate_pct` — confirm emit volume drops **without pod restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [HIPAA audit sampling architecture](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-hipaa-audit-sampling.md)
- Related: [PHI access capture tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-phi-access-capture-tuning.md)

---

*Kiponos.io — compliance prose lives in the packet; sample_rate_pct lives in the tree.*