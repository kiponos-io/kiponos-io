---
title: "Velocity Limits Live — Card Authorization Thresholds Ops Can Tune (Java SDK)"
published: false
tags: java, fintech, compliance, architecture
description: PCI velocity checks as live thresholds — RegOps agility for fraud response, not compliance certification.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-regops-velocity-limits-live.md
main_image: https://files.catbox.moe/574r3o.jpg
---

Sunday 04:09 UTC. Fraud monitoring flags a **BIN-testing ring** — 9,200 micro-authorizations against prepaid BIN ranges in seventy minutes. The analyst knows the fix: drop `max_auth_per_hour` from 12 to 3 for the attacked segment. The authorization service still enforces `MAX_AUTH_PER_HOUR = 12` from `@Value` injection at startup.

The bridge channel lights up. Compliance will not approve a production deploy until Monday. Chargeback exposure compounds. The fraud lead asks:

> "We need **ops velocity** on velocity limits — not a **compliance deploy**. Why is our fraud response gated on CAB?"

**Honest framing:** this article covers **operational threshold tuning** for card authorization velocity. Kiponos lets fraud and platform ops tighten `max_auth_per_hour` while JVMs authorize. It does **not** replace PCI DSS attestation, acquirer contracts, HSM key management, or your formal change-advisory program. The tree holds **live limits** — not certification status.

Most Java card services encode velocity as **three artifacts**: a compliance spreadsheet, static `application-pci.yml`, and `private static final int MAX_AUTH_PER_HOUR`. [Kiponos.io](https://kiponos.io) collapses runtime ceilings into profile `['cards']['prod']['velocity']` — global caps, BIN-segment overrides, and emergency multipliers — with **local `get*()` on every auth**.

## The problem: max_auth_per_hour frozen on the authorization path

```java
@Service
public class CardVelocityGate {
    private static final int MAX_AUTH_PER_HOUR = 12;

    public AuthDecision authorize(CardPan pan, Money amount) {
        int count = velocityRedis.countHour(pan);
        if (count >= MAX_AUTH_PER_HOUR) {
            return AuthDecision.decline("velocity_exceeded");
        }
        return AuthDecision.approve();
    }
}
```

Policy scattered in restart-bound YAML:

```yaml
# application-cards-prod.yml
cards:
  prod:
    velocity:
      max_auth_per_hour: 12
      bin_overrides: {}
```

During a BIN attack you need to:

1. Lower **`velocity.bin_prepaid.max_auth_per_hour`** immediately
2. Flip **`emergency.tighten_all`** with a global multiplier
3. Raise **`stepup.score_threshold`** so marginal auths route to review

Helm + CAB while testers iterate BINs is **policy theater with chargeback interest**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Velocity limits are compliance constants — deploy is correct" | Card testers move faster than CAB calendars |
| "Fraud SaaS console is enough" | Authorizer JVM still reads stale YAML |
| "Per-BIN limits belong in the database" | JDBC on every auth adds latency |
| "Emergency override is a break-glass env var" | Rolling restart mid-attack loses sessions |
| "Staging limits match prod" | BIN override keys never copied to staging |

## The Aha

**`max_auth_per_hour` is operational config** — it changes during BIN attacks, issuer outages, and post-breach hardening. It belongs in a **live tree** read with `kiponos.path("velocity").getInt("max_auth_per_hour")`, not a constant compiled at boot.

## What Kiponos.io is for RegOps velocity limits

[Kiponos.io](https://kiponos.io) connects once per JVM over WebSocket. Profile `['cards']['prod']['velocity']` loads into an **in-memory cache**. Dashboard edits send **deltas**; the next authorization reads limits locally.

`afterValueChanged` logs who tightened ceilings, forwards events to SIEM, and increments `velocity_limit_change_total` — **without** restarting auth pods.

**RegOps boundary:** dashboard ACL proves **who moved operational limits** — supporting your PCI change evidence. Kiponos does **not** certify PCI scope or replace QSA review.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/z0k9tf.png)

## Config tree — velocity, BIN segments, emergency, step-up

Five folders — `velocity`, `bin_prepaid`, `bin_corporate`, `emergency`, `audit`:

```yaml
velocity/
  max_auth_per_hour: 12
  max_amount_usd_per_day: 2500
  max_declines_per_hour: 8
  enabled: true
bin_prepaid/
  max_auth_per_hour: 12
  max_amount_usd_per_hour: 50
  enabled: true
bin_corporate/
  max_auth_per_hour: 40
  max_amount_usd_per_hour: 5000
  enabled: true
emergency/
  tighten_all: false
  global_multiplier: 0.25
  auto_on_fraud_score: 88
stepup/
  score_threshold: 70
  force_all: false
audit/
  last_change_by: ""
  siem_forward_enabled: true
```

Profile path: `['cards']['prod']['velocity']`.

## Java integration: live velocity gate + afterValueChanged

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.stereotype.Service;

@Service
public class RegOpsVelocityGate {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final VelocityStore velocityStore;

    public RegOpsVelocityGate(VelocityStore velocityStore) {
        this.velocityStore = velocityStore;
        kiponos.afterValueChanged(change -> {
            log.info("Velocity limit delta: path={} value={}", change.path(), change.newValue());
            if (kiponos.path("audit").getBool("siem_forward_enabled")) {
                siemClient.emit("regops_velocity_change", change.path(), change.newValue());
            }
        });
    }

    public AuthDecision authorize(CardPan pan, Money amount, int fraudScore, BinSegment segment) {
        var emergency = kiponos.path("emergency");
        if (emergency.getBool("tighten_all")
            || fraudScore >= emergency.getInt("auto_on_fraud_score")) {
            return checkWithMultiplier(pan, segment, emergency.getFloat("global_multiplier"));
        }

        int cap = resolveHourlyCap(segment);
        if (velocityStore.countLastHour(pan) >= cap) {
            return AuthDecision.decline("velocity_hourly_exceeded");
        }

        var stepup = kiponos.path("stepup");
        if (fraudScore >= stepup.getInt("score_threshold") || stepup.getBool("force_all")) {
            return AuthDecision.stepUp("fraud_score_review");
        }
        return AuthDecision.approve();
    }

    private int resolveHourlyCap(BinSegment segment) {
        String folder = switch (segment) {
            case PREPAID -> "bin_prepaid";
            case CORPORATE -> "bin_corporate";
            default -> null;
        };
        if (folder != null && kiponos.path(folder).getBool("enabled")) {
            return kiponos.path(folder).getInt("max_auth_per_hour");
        }
        return kiponos.path("velocity").getInt("max_auth_per_hour");
    }

    private AuthDecision checkWithMultiplier(CardPan pan, BinSegment segment, float mult) {
        int cap = Math.max(1, Math.round(resolveHourlyCap(segment) * mult));
        if (velocityStore.countLastHour(pan) >= cap) {
            return AuthDecision.decline("emergency_velocity_exceeded");
        }
        return AuthDecision.approve();
    }
}
```

Velocity **counts** stay in Redis; **limits** live in Kiponos memory — the right separation for PCI scope.

## Real-world scenarios

| Scenario | Without live velocity tree | With Kiponos RegOps limits |
|----------|---------------------------|----------------------------|
| BIN-testing ring overnight | CAB-gated deploy; hours of exposure | `bin_prepaid/max_auth_per_hour: 3` live |
| Cross-BIN attack wave | Manual env var per pod | `emergency/tighten_all: true` |
| Post-incident restore | Second deploy | Reset emergency + BIN caps in dashboard |
| Step-up flood after breach news | Rebuild with new threshold | `stepup/score_threshold: 65` live |
| QSA asks who tightened limits | Git + deploy logs | Kiponos ACL + SIEM deltas |

## Performance: velocity limits on the auth hot path

- **One WebSocket per auth JVM** — not one config fetch per authorization
- **Cap resolution is ≤3 local reads** — nanoseconds vs issuer RTT
- **Delta patches** — one MCC/BIN key change, not full tree to every pod
- **No JDBC for policy** — limits in memory; counts in your existing store
- **No YAML re-parse** during fraud surges at 20k auths/sec

## Compare to alternatives

| Approach | Mid-attack tighten | Hot-path latency | BIN + emergency in one tree |
|----------|-------------------|------------------|----------------------------|
| application-pci.yml + CAB | No — hours | Static until restart | Partial |
| Fraud SaaS only | Yes in SaaS; JVM stale | Two sources of truth | No |
| Database policy table | Yes with JDBC | Milliseconds per auth | Possible |
| Redis config poll | Yes | Poll jitter | Custom schema |
| **Kiponos SDK** | **Seconds + ACL** | **Zero (in-process)** | **Yes** |

## When not to use Kiponos for velocity limits

| Boundary | Better home |
|----------|-------------|
| PAN encryption, HSM PIN blocks, tokenization secrets | HSM / Vault |
| PCI network segmentation, firewall rules | Infrastructure |
| Whether your velocity program satisfies PCI | QSA + compliance — not this article |
| Rolling velocity **counts** per PAN | Redis / fraud store |
| Acquirer endpoint certificates | Cert manager / GitOps |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['cards']['prod']['velocity']` with folders above. ACL: fraud + platform ops.
2. Add `io.kiponos:sdk-boot-3` to your authorization service.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, `-Dkiponos="['cards']['prod']['velocity']"`.
4. Replace `MAX_AUTH_PER_HOUR` with `kiponos.path("velocity").getInt("max_auth_per_hour")` and BIN overrides.
5. Wire `afterValueChanged` SIEM forwarding in `RegOpsVelocityGate`.
6. Tabletop: staging BIN attack — lower `bin_prepaid/max_auth_per_hour` and confirm declines rise **without restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [PCI velocity checks architecture](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-pci-velocity-checks.md)
- Related: [GitOps vs live operational config](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-gitops-vs-live-config.md)

---

*Kiponos.io — PCI policy prose lives in the packet; max_auth_per_hour lives in the tree.*