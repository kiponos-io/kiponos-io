---
title: "PHI Access Capture Tuning — Live Sampling for Health APIs (Java SDK)"
published: false
tags: java, healthcare, compliance, architecture
description: Audit capture intensity for health APIs — honest ops agility, not HIPAA certification.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-phi-access-capture-tuning.md
main_image: https://files.catbox.moe/574r3o.jpg
---

Saturday 06:18 UTC. Weekend on-call watches **OpenSearch ingest** climb on the EHR access-log index — read-heavy `/Patient` and `/Observation` endpoints dominate. The runbook says reduce capture on read paths to 5% until Monday's index rollover. Every FHIR pod still uses `PHI_SAMPLE_PCT = 20` from `application-ehr.yml`.

The clinical informatics lead clarifies in Slack:

> "We are **not** changing HIPAA policy language — we need **ops** to dial down **access_log_sample_pct** on chart reads until storage catches up. Why does that need a **rolling restart**?"

**Honest framing:** Kiponos lets platform ops tune **`access_log_sample_pct`** per endpoint class while FHIR APIs serve patients. This is **operational capture intensity** — not HIPAA certification, not a BAA amendment, and not substitute for your security risk assessment. Compliance prose stays in your packet; the tree holds **what you log right now**.

## The problem: access_log_sample_pct frozen on the FHIR read path

```java
@Component
public class PhiAccessLogFilter implements Filter {
    private static final int ACCESS_LOG_SAMPLE_PCT = 20;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        if (isPhiRead(http) && shouldSample(ACCESS_LOG_SAMPLE_PCT)) {
            accessLogger.emit(buildPhiAccessEvent(http));
        }
        chain.doFilter(req, res);
    }
}
```

Static config:

```yaml
ehr:
  prod:
    phi:
      access_log_sample_pct: 20
```

During weekend storage pressure you need to:

1. Lower **`endpoints.read.access_log_sample_pct`** to 5
2. Keep **`endpoints.write.access_log_sample_pct`** at 100
3. Enable **`weekend_throttle.mode`** with automatic restore Monday 06:00

Recycling FHIR pods during active chart loads is **ops friction** — not patient care improvement.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "PHI capture rate is fixed by compliance" | Compliance sets bounds; ops tunes within them during incidents |
| "We'll scale OpenSearch" | Replicas do not change JVM sample percent without restart |
| "Audit belongs in the SIEM only" | Events never emitted cannot be recovered downstream |
| "Write and read paths share one rate" | Read storms need different capture than writes |
| "Weekend throttle is a cron job" | Cron changes deploy schedules — not live JVM constants |

## The Aha

**`access_log_sample_pct` is operational config** — it changes during storage incidents, read surges, and targeted investigations. It belongs in profile `['ehr']['prod']['phi']` with local `getInt()` on every PHI access decision.

## What Kiponos.io is for PHI capture tuning (RegOps)

[Kiponos.io](https://kiponos.io) hydrates `['ehr']['prod']['phi']` into each FHIR service JVM. Dashboard deltas update capture rates; reads stay local.

`afterValueChanged` logs capture policy changes and forwards meta-audit events — **without** recycling clinical API pods.

**RegOps boundary:** Kiponos ACL documents **who throttled capture** — supporting operational evidence. It does **not** certify HIPAA compliance or replace OCR-ready policy documentation.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/wfhlfa.png)

## Config tree — phi, endpoints, weekend throttle, redaction

Five folders — `phi`, `endpoints`, `weekend_throttle`, `redaction`, `meta`:

```yaml
phi/
  access_log_sample_pct: 20
  min_sample_pct: 1
  max_sample_pct: 100
  emit_async: true
endpoints/
  read/
    access_log_sample_pct: 20
    path_patterns: ["/Patient", "/Observation", "/DiagnosticReport"]
  write/
    access_log_sample_pct: 100
    path_patterns: ["/Patient", "/MedicationRequest"]
weekend_throttle/
  mode_enabled: false
  read_sample_pct: 5
  auto_restore_cron_utc: "0 6 * * 1"
redaction/
  mask_fields: ["ssn", "mrn"]
  include_resource_type: true
meta/
  last_change_by: ""
  siem_forward_enabled: true
```

Profile path: `['ehr']['prod']['phi']`.

## Java integration: live PHI capture + afterValueChanged

```java
import io.kiponos.sdk.Kiponos;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RegOpsPhiCaptureFilter implements Filter {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final PhiAccessLogger accessLogger;

    public RegOpsPhiCaptureFilter(PhiAccessLogger accessLogger) {
        this.accessLogger = accessLogger;
        kiponos.afterValueChanged(change -> {
            log.info("PHI capture delta: path={} value={}", change.path(), change.newValue());
            if (kiponos.path("meta").getBool("siem_forward_enabled")) {
                siemClient.emit("regops_phi_capture_change", change.path(), change.newValue());
            }
        });
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        if (isPhiPath(http) && shouldCapture(http)) {
            accessLogger.emitAsync(buildEvent(http));
        }
        chain.doFilter(req, res);
    }

    private boolean shouldCapture(HttpServletRequest http) {
        int pct = resolveSamplePct(http);
        return ThreadLocalRandom.current().nextInt(100) < pct;
    }

    private int resolveSamplePct(HttpServletRequest http) {
        var weekend = kiponos.path("weekend_throttle");
        if (weekend.getBool("mode_enabled") && isReadPath(http)) {
            return weekend.getInt("read_sample_pct");
        }
        if (isWritePath(http)) {
            return kiponos.path("endpoints", "write").getInt("access_log_sample_pct");
        }
        if (isReadPath(http)) {
            return kiponos.path("endpoints", "read").getInt("access_log_sample_pct");
        }
        return kiponos.path("phi").getInt("access_log_sample_pct");
    }
}
```

## Real-world scenarios

| Scenario | Without live PHI tree | With Kiponos RegOps capture |
|----------|----------------------|----------------------------|
| Weekend OpenSearch surge | FHIR pod restart; chart retries | `weekend_throttle/mode_enabled: true` |
| Investigation on writes only | Global rate change affects reads | `endpoints/write` stays at 100% |
| Post-rollover Monday restore | Second deploy | Disable weekend throttle in dashboard |
| FinOps cost review | Spreadsheet estimates | Live keys tied to ingest metrics |
| Security asks who lowered capture | Deploy tickets | Kiponos ACL + SIEM stream |

## Performance: PHI capture on the clinical path

- **One WebSocket per FHIR JVM** — not per Patient read
- **Sample decision ≤4 local reads** — nanoseconds vs HAPI store I/O
- **Delta patches** — one endpoint key, not full tree reload to 40 pods
- **`emit_async: true`** — capture policy off critical latency path
- **No YAML re-parse** during read-heavy weekend traffic

## Compare to alternatives

| Approach | Weekend throttle | Hot-path latency | Read vs write split |
|----------|-------------------|------------------|---------------------|
| application-ehr.yml + restart | No | Static until restart | Awkward |
| SIEM-only capture | Misses unsampled events | N/A at JVM | No |
| DB policy table | Yes with JDBC | Costly per read | Possible |
| **Kiponos SDK** | **Seconds** | **Zero (in-process)** | **Yes** |

## When not to use Kiponos for PHI capture

| Boundary | Better home |
|----------|-------------|
| HIPAA policy documents and BAAs | Legal / compliance wiki |
| PHI encryption, TLS, break-glass credentials | HSM / Vault |
| Whether your sampling satisfies OCR expectations | Compliance officer — not this article |
| Long-term immutable audit storage | WORM / SIEM architecture |
| Patient consent and treatment authorization | EHR database |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['ehr']['prod']['phi']`.
2. Add `io.kiponos:sdk-boot-3` to FHIR API service.
3. Set `-Dkiponos="['ehr']['prod']['phi']"`.
4. Replace `ACCESS_LOG_SAMPLE_PCT` with endpoint-aware `getInt()` reads.
5. Wire `afterValueChanged` SIEM forwarding.
6. Drill: staging — enable `weekend_throttle` and confirm read capture drops **without restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [HIPAA audit sampling architecture](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-hipaa-audit-sampling.md)
- Related: [Audit log sampling rates live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-audit-log-sampling-rates.md)

---

*Kiponos.io — HIPAA policy prose lives in the packet; access_log_sample_pct lives in the tree.*