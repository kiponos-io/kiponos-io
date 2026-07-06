---
title: "PCI Velocity Checks as Live Thresholds — Tune Card Authorization Limits Without a Compliance Deploy (Java SDK)"
published: false
tags: java, pci, architecture, fintech
description: Velocity limits frozen in application.yml mean fraud spikes need emergency deploys. Kiponos holds per-MCC thresholds, block scores, and override flags in one live tree — risk ops tunes limits while JVMs keep running.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-pci-velocity-checks.md
main_image: https://files.catbox.moe/574r3o.jpg
---

Sunday 04:17 UTC. Fraud ops detects a **card-testing ring** — 14,000 micro-authorizations against MCC 5816 in ninety minutes. The runbook says step 3: *"lower `MAX_AUTH_PER_CARD_PER_HOUR` from 25 to 8 in `application-pci.yml`, open PR, wait for change advisory board sign-off."*

The ring is still running. Compliance will not approve a production deploy until business hours. The authorization service still permits 25 auths per PAN per hour because the limit is a `@Value` injected at startup. Chargeback exposure compounds every minute.

The CISO asks:

> "Why does our **fraud response** wait on a **deploy pipeline** when PCI scope already lives in this JVM?"

Most Java card authorization services encode velocity policy as **three different artifacts**: a compliance spreadsheet, static YAML per environment, and hard-coded `MAX_AUTH_PER_HOUR` constants reviewed quarterly. [Kiponos.io](https://kiponos.io) collapses that into **one operational tree** — per-MCC velocity ceilings, block thresholds, and emergency override flags — readable on every authorization with **local `get*()` calls** and tunable from the dashboard while processes run.

## The problem: velocity limits baked into immutable PCI config

A typical authorization gate checks velocity like this:

```java
@Service
public class VelocityGate {
    private static final int MAX_AUTH_PER_CARD_PER_HOUR = 25;
    private static final int MAX_AMOUNT_USD_PER_DAY = 5000;

    public VelocityResult check(CardPan pan, Money amount, String mcc) {
        int hourlyCount = velocityStore.countLastHour(pan);
        if (hourlyCount >= MAX_AUTH_PER_CARD_PER_HOUR) {
            return VelocityResult.decline("velocity_hourly_exceeded");
        }
        return VelocityResult.approve();
    }
}
```

Velocity policy usually lives elsewhere — scattered and static:

```yaml
# application-pci-prod.yml — requires rolling restart + CAB approval
pci:
  velocity:
    max_auth_per_card_per_hour: 25
    max_amount_usd_per_day: 5000
    mcc_5816_multiplier: 1.0
```

Or worse — different limits per region in forked files nobody keeps aligned:

```java
// Compiled differently per AZ because values-prod-us.yml diverged
private static final int MAX_AUTH_PER_CARD_PER_HOUR = 25;
```

The authorization path executes **tens of thousands of times per second**. During a card-testing surge you need to:

1. Lower **`velocity.mcc_5816.max_auth_per_hour`** immediately for the attacked MCC
2. Flip **`velocity.emergency_mode`** to apply stricter global ceilings
3. Raise **`velocity.block_score_threshold`** so marginal auths route to step-up

Doing that through CAB-gated deploys while testers iterate BINs is not PCI compliance — it is **policy theater with chargeback interest**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Velocity limits are compliance constants — deploy is correct" | Fraud moves faster than CAB calendars |
| "We'll tune in the fraud SaaS console" | Authorizer JVM still reads local YAML — two sources of truth |
| "Per-MCC limits belong in the database" | JDBC on every auth adds latency and connection pressure |
| "Emergency override is a break-glass env var" | Rolling restart during active attack loses in-flight sessions |
| "Staging limits match prod" | MCC 5816 multiplier never copied to staging tree |

## The architecture insight

**PCI velocity thresholds are operational config, not compliance archaeology.** The same knobs your fraud runbook tells analysts to edit — hourly auth caps, MCC multipliers, emergency mode — belong in **one live tree** the JVM already reads on every authorization. Kiponos makes "tighten MCC 5816 to 8/hour" a **dashboard edit with ACL audit**, not a deploy pipeline.

## What Kiponos.io is for PCI velocity checks

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Each Spring Boot authorization service connects **once** at startup over WebSocket; the profile tree — for example `['payments']['pci']['prod']['live']` — loads into an **in-memory cache** inside the Java SDK.

When fraud ops sets `velocity.mcc_5816.max_auth_per_hour` to `8`, a **delta** patches only that key. The next `kiponos.path("velocity", "mcc_5816").getInt("max_auth_per_hour")` on an incoming authorization is a **local memory read** — no HTTP to a config API, no JDBC poll, no Redis round-trip on the card path.

`afterValueChanged` listeners let you log audit trails to your SIEM, increment `velocity_threshold_change_total`, and notify compliance **without** restarting the JVM.

No restart. No redeploy. No `@RefreshScope` bean recycle.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/0xwzfh.png)

**Compliance docs describe intent; the tree enforces live limits.** Keep policy prose in your PCI packet — but the **authoritative velocity values** live in Kiponos where tightening them takes seconds with dashboard ACL.

## Config tree — velocity, MCC overrides, and emergency mode

Five folders — `velocity`, `mcc`, `emergency`, `stepup`, `audit`:

```yaml
velocity/
  global/
    max_auth_per_card_per_hour: 25
    max_amount_usd_per_day: 5000
    max_declines_per_hour: 10
  mcc_5816/
    max_auth_per_hour: 25
    max_amount_usd_per_hour: 200
    enabled: true
  mcc_5411/
    max_auth_per_hour: 40
    max_amount_usd_per_hour: 800
emergency/
  emergency_mode: false
  global_multiplier: 0.5
  auto_enable_on_fraud_score: 85
stepup/
  block_score_threshold: 72
  review_score_threshold: 58
  force_stepup_all: false
audit/
  last_velocity_change_by: ""
  last_velocity_change_at_ms: 0
  siem_forward_enabled: true
```

One tree. One profile path: `['payments']['pci']['prod']['live']`. Staging card-testing rehearsals share **identical key layout** — only values differ.

## Java integration: velocity gate + live MCC ceilings

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;

@Service
public class PciVelocityGate {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final VelocityStore velocityStore;

    public PciVelocityGate(VelocityStore velocityStore) {
        this.velocityStore = velocityStore;
        kiponos.afterValueChanged(change -> {
            log.info("PCI velocity delta: path={} value={}", change.path(), change.newValue());
            if (kiponos.path("audit").getBool("siem_forward_enabled")) {
                siemClient.emit("pci_velocity_change", change.path(), change.newValue());
            }
        });
    }

    public VelocityResult check(CardPan pan, Money amount, String mcc, int fraudScore) {
        var emergency = kiponos.path("emergency");
        if (emergency.getBool("emergency_mode")
            || fraudScore >= emergency.getInt("auto_enable_on_fraud_score")) {
            return checkWithEmergencyMultiplier(pan, amount, mcc);
        }

        int hourlyCap = resolveHourlyCap(mcc);
        int hourlyCount = velocityStore.countLastHour(pan);

        if (hourlyCount >= hourlyCap) {
            return VelocityResult.decline("velocity_hourly_exceeded");
        }

        var stepup = kiponos.path("stepup");
        if (fraudScore >= stepup.getInt("block_score_threshold")) {
            return VelocityResult.decline("fraud_score_block");
        }
        if (fraudScore >= stepup.getInt("review_score_threshold")
            || stepup.getBool("force_stepup_all")) {
            return VelocityResult.stepUp("fraud_score_review");
        }

        return VelocityResult.approve();
    }

    private int resolveHourlyCap(String mcc) {
        var mccPath = kiponos.path("velocity", "mcc_" + mcc);
        if (mccPath.getBool("enabled", false)) {
            return mccPath.getInt("max_auth_per_hour");
        }
        return kiponos.path("velocity", "global").getInt("max_auth_per_card_per_hour");
    }

    private VelocityResult checkWithEmergencyMultiplier(CardPan pan, Money amount, String mcc) {
        float mult = kiponos.path("emergency").getFloat("global_multiplier");
        int baseCap = resolveHourlyCap(mcc);
        int effectiveCap = Math.max(1, Math.round(baseCap * mult));
        if (velocityStore.countLastHour(pan) >= effectiveCap) {
            return VelocityResult.decline("emergency_velocity_exceeded");
        }
        return VelocityResult.approve();
    }
}
```

Every `getInt()`, `getBool()`, and `getFloat()` on the authorization path is **O(1) local cache** — microseconds, not cross-region config service RTT.

Dashboard ACL records **who** changed `mcc_5816.max_auth_per_hour` — satisfying PCI change-management evidence without a deploy ticket.

## Real-world scenarios

| Scenario | Without live velocity tree | With Kiponos one-tree PCI policy |
|----------|---------------------------|----------------------------------|
| Card-testing ring on MCC 5816 | CAB-gated deploy; hours of exposure | Dashboard: `max_auth_per_hour: 8` live |
| BIN attack across all MCCs | Manual env var per pod | `emergency_mode: true` + `global_multiplier: 0.5` |
| Step-up flood after breach news | Rebuild with new score threshold | `block_score_threshold: 68` in dashboard |
| Post-incident restore | Second deploy wave | Reset emergency flag; restore MCC caps |
| QSA audit trail | Git commits + deploy logs | Kiponos ACL + `afterValueChanged` SIEM events |

## Performance: why velocity gates must not add network I/O

- **One WebSocket per JVM** — not one config fetch per authorization
- **Velocity cap resolution is three local reads** — nanoseconds vs issuer network RTT
- **Delta patches** — tightening one MCC sends one patch, not full tree reload to every auth pod
- **No JDBC for policy** — limits live in memory; velocity **counts** still use your existing store
- **No GC pressure** from re-parsing PCI YAML on every auth during fraud surges

In load tests, Kiponos reads are noise on the authorization path; card network and HSM latency dominate.

## Compare to alternatives

| Approach | Mid-attack threshold tighten | Hot-path read latency | Single tree for MCC + emergency + step-up |
|----------|-----------------------------|----------------------|-------------------------------------------|
| application-pci.yml + CAB deploy | No — hours | Zero (static) but stale | Partial — restart required |
| Fraud SaaS console only | Yes in SaaS | Authorizer still stale | No — two sources of truth |
| Database policy table | Yes with JDBC | Milliseconds per auth — costly | Possible — schema discipline |
| Redis config hash | Yes with poll | Poll interval adds tail latency | Possible — custom schema |
| Feature-flag SaaS | Booleans only | SDK network on evaluation | No — per-MCC integers awkward |
| **Kiponos SDK** | **Yes — dashboard delta + ACL** | **Zero (in-process cache)** | **Yes — one profile tree** |

## When not to use Kiponos for PCI velocity

| Boundary | Better home |
|----------|-------------|
| PAN encryption keys, HSM PIN blocks, tokenization secrets | HSM / Vault — never live dashboard |
| Cardholder data storage retention periods | Compliance policy docs + DBA tooling |
| PCI network segmentation and firewall rules | Infrastructure / network compliance |
| Immutable acquirer endpoint certificates | GitOps / cert manager |
| Velocity **count** state per PAN (rolling windows) | Redis / dedicated fraud store — not config hub |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — add profile `['payments']['pci']['prod']['live']` with `velocity`, `emergency`, and `stepup` folders matching the tree above. Restrict dashboard ACL to fraud + compliance roles.
2. Add `io.kiponos:sdk-boot-3` to your Spring Boot authorization service.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['payments']['pci']['prod']['live']"`.
4. Replace `MAX_AUTH_PER_CARD_PER_HOUR` constants with `kiponos.path("velocity", ...).getInt(...)`.
5. Register `afterValueChanged` SIEM forwarding and implement `PciVelocityGate` on the auth hot path.
6. Tabletop: in staging, simulate card-testing and lower `mcc_5816.max_auth_per_hour` — confirm declines increase **without pod restart**. Document key names in your fraud runbook.

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Multi-region active-active bounds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-multi-region-active-active.md)
- Related: [Cost control runtime](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-cost-control-runtime.md)

---

*Kiponos.io — PCI policy prose lives in the packet; velocity limits live in the tree.*