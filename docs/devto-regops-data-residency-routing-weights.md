---
title: "Data Residency Routing Weights — Shift Traffic Posture Live (Java SDK)"
published: false
tags: java, compliance, architecture, devops
description: Region weights in static config cannot track regulatory guidance changes intra-day. Kiponos holds routing weights — operational posture, not legal advice.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-data-residency-routing-weights.md
main_image: https://files.catbox.moe/574r3o.jpg
---

Monday 11:27 CET. Legal publishes an **interim data-processing memo** — EU customer traffic should prefer `eu-west-1` processing while a US-region vendor patch rolls through. The edge router still reads `euWeight: 100` from `routing-residency.yml` deployed three weeks ago when both stacks were symmetric.

Traffic engineering opens a P1 to shift `eu_traffic_weight` toward the EU stack **without** draining the US fleet mid-business-day. The platform lead asks:

> "Residency is a **legal posture** — but **weighting** is an **ops knob**. Why does nudging EU preference require a **full config redeploy**?"

**Honest framing:** Kiponos lets ops tune **`eu_traffic_weight`** and related routing integers while gateways run. This is **operational traffic posture** — not legal advice, GDPR certification, or substitute for counsel-reviewed data-transfer agreements. Your DPA and residency matrix stay in legal docs; the tree holds **where traffic goes right now**.

Most Java API gateways encode residency routing as **three artifacts**: a legal residency matrix PDF, static `application-routing.yml`, and `private static final int EU_WEIGHT = 100` in the routing filter. [Kiponos.io](https://kiponos.io) unifies runtime weights in profile `['routing']['prod']['residency']`.

## The problem: eu_traffic_weight baked into immutable gateway config

```java
@Component
public class ResidencyRouter implements Filter {
    private static final int EU_TRAFFIC_WEIGHT = 100;
    private static final int US_TRAFFIC_WEIGHT = 0;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String region = weightedPick(EU_TRAFFIC_WEIGHT, US_TRAFFIC_WEIGHT);
        requestContext.setTargetRegion(region);
        chain.doFilter(req, res);
    }
}
```

Static YAML requires gateway recycle:

```yaml
routing:
  prod:
    residency:
      eu_traffic_weight: 100
      us_traffic_weight: 0
```

During a vendor patch window you need to:

1. Raise **`eu_traffic_weight`** while US stack receives patches
2. Lower **`us_traffic_weight`** without zeroing failover capacity
3. Enable **`failover.allow_cross_region`** only for health-checked paths

Recycling forty gateway pods during EU business hours is **routing theater** — clients see 503s while legal only asked for a **weight nudge**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Residency weights are legal constants" | Legal sets intent; ops tunes weights hourly during incidents |
| "Global load balancer handles this" | App-layer residency tags still read JVM constants |
| "We'll update weights in the next release train" | Vendor patches do not wait for sprint boundaries |
| "Feature flags can route by cohort" | Float weights per region are awkward in flag SaaS |
| "GeoDNS is sufficient" | In-region processing rules live in application code |

## The Aha

**`eu_traffic_weight` is operational config** — it shifts during vendor maintenance, regional outages, and interim legal guidance. It belongs in a **live tree** the gateway reads with `kiponos.path("residency").getInt("eu_traffic_weight")`, not YAML that needs a recycle.

## What Kiponos.io is for residency routing (RegOps)

[Kiponos.io](https://kiponos.io) hydrates profile `['routing']['prod']['residency']` over WebSocket into each gateway JVM. Dashboard edits patch **deltas**; the next routing decision uses local `getInt()`.

`afterValueChanged` logs weight changes, notifies traffic engineering, and increments `residency_weight_change_total`.

**RegOps boundary:** Kiponos ACL shows **who shifted weights** — useful operational evidence. It does **not** determine lawful basis for transfers or replace privacy impact assessments.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/5q8fdx.png)

## Config tree — residency, regions, failover, audit

Five folders — `residency`, `regions`, `failover`, `health`, `meta`:

```yaml
residency/
  eu_traffic_weight: 100
  us_traffic_weight: 0
  apac_traffic_weight: 0
  weight_normalization: auto
regions/
  eu/
    endpoint_pool: eu-west-1-primary
    min_weight_floor: 80
  us/
    endpoint_pool: us-east-1-primary
    min_weight_floor: 0
failover/
  allow_cross_region: false
  cross_region_max_weight: 15
  health_gate_enabled: true
health/
  us_stack_degraded: false
  degrade_us_weight_to: 5
  auto_shift_on_degrade: true
meta/
  last_change_by: ""
  siem_forward_enabled: true
```

Profile path: `['routing']['prod']['residency']`.

## Java integration: live residency router + afterValueChanged

```java
import io.kiponos.sdk.Kiponos;
import jakarta.servlet.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RegOpsResidencyRouter implements Filter {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public RegOpsResidencyRouter() {
        kiponos.afterValueChanged(change -> {
            log.info("Residency weight delta: path={} value={}", change.path(), change.newValue());
            if (kiponos.path("meta").getBool("siem_forward_enabled")) {
                siemClient.emit("regops_residency_change", change.path(), change.newValue());
            }
        });
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String region = pickRegion();
        RequestContext.setTargetRegion(region);
        chain.doFilter(req, res);
    }

    private String pickRegion() {
        int euW = effectiveWeight("eu");
        int usW = effectiveWeight("us");
        int apacW = kiponos.path("residency").getInt("apac_traffic_weight");
        int total = euW + usW + apacW;
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));

        if (roll < euW) return "eu";
        if (roll < euW + usW) return "us";
        return "apac";
    }

    private int effectiveWeight(String region) {
        var residency = kiponos.path("residency");
        int base = residency.getInt(region + "_traffic_weight");

        var health = kiponos.path("health");
        if ("us".equals(region) && health.getBool("us_stack_degraded")
            && health.getBool("auto_shift_on_degrade")) {
            return health.getInt("degrade_us_weight_to");
        }

        var failover = kiponos.path("failover");
        if (!failover.getBool("allow_cross_region") && !"eu".equals(region)) {
            return Math.min(base, failover.getInt("cross_region_max_weight"));
        }

        int floor = kiponos.path("regions", region).getInt("min_weight_floor");
        return Math.max(base, floor);
    }
}
```

Every weight read is **local memory** — microseconds on a path that already does TLS and auth.

## Real-world scenarios

| Scenario | Without live residency tree | With Kiponos RegOps weights |
|----------|----------------------------|----------------------------|
| US vendor patch window | Gateway redeploy; EU 503 blips | `eu_traffic_weight: 95` live |
| US stack health degrade | Manual DNS flip | `health/us_stack_degraded: true` auto-shifts |
| Post-patch restore | Second deploy wave | Reset weights in dashboard |
| Interim legal memo | Spreadsheet to SRE | Documented key names in runbook |
| Auditor asks who shifted EU traffic | Deploy tickets | Kiponos ACL + SIEM deltas |

## Performance: residency weights on the gateway path

- **One WebSocket per gateway JVM** — not per-request config HTTP
- **Weight resolution is ≤6 local reads** — nanoseconds vs upstream RTT
- **Delta patches** — one integer change propagates without full tree reload
- **No DNS propagation wait** for intra-day posture nudges
- **Identical tree shape** in staging for residency drills

## Compare to alternatives

| Approach | Intra-day weight shift | Hot-path latency | Failover + health in one tree |
|----------|------------------------|------------------|------------------------------|
| routing.yml + restart | No — pod recycle | Static until restart | Partial |
| GeoDNS only | Minutes TTL | N/A at app layer | No in-app failover flags |
| Global LB console | Yes at edge | App tags still stale | Split brain |
| Feature-flag SaaS | Cohort booleans | Network per eval | Poor for weight integers |
| **Kiponos SDK** | **Seconds + ACL** | **Zero (in-process)** | **Yes** |

## When not to use Kiponos for residency routing

| Boundary | Better home |
|----------|-------------|
| Lawful basis for international transfers | Legal counsel + DPA |
| Whether EU weighting satisfies Schrems II posture | Privacy office — not this article |
| Physical data store region for backups | Infra / DBA / cloud console |
| TLS certs and mTLS trust stores | Cert manager / Vault |
| DNS apex records and anycast | Traffic engineering GitOps |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['routing']['prod']['residency']` with folders above.
2. Add `io.kiponos:sdk-boot-3` to your API gateway service.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, `-Dkiponos="['routing']['prod']['residency']"`.
4. Replace `EU_TRAFFIC_WEIGHT` constants with `kiponos.path("residency").getInt("eu_traffic_weight")`.
5. Register `RegOpsResidencyRouter` and `afterValueChanged` SIEM forwarding.
6. Drill: staging — shift `eu_traffic_weight` and confirm trace tags show EU preference **without gateway restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Multi-region active-active bounds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-multi-region-active-active.md)
- Related: [Change window posture trees](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-change-window-posture-trees.md)

---

*Kiponos.io — legal residency intent lives in the packet; eu_traffic_weight lives in the tree.*