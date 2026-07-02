---
title: "Chaos Blast Radius as Live Config — Shrink Failure Injection Without Redeploying Experiment Pods (Java SDK)"
published: false
tags: java, chaos, architecture, resilience
description: Chaos experiment scope frozen in YAML means aborting a game day requires pod restarts. Kiponos holds blast radius, target percentages, and kill-switch flags in one live tree — SRE shrinks scope while JVMs keep running.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-chaos-blast-radius.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-arch-chaos-blast-radius.jpg
---

Wednesday 15:08 UTC. The platform team runs **Game Day 47** — inject 800ms latency on `inventory-api` calls from **10%** of checkout pods. Within twenty minutes, cart abandonment spikes beyond the rehearsal budget. The chaos engineer opens the runbook: step 6 says *"set `BLAST_PCT=0` in `chaos-values.yaml`, commit, rolling restart experiment sidecars."*

Checkout SLO is burning. The experiment controller still reads `BLAST_PCT = 10` from a `@Value` injected at bean creation. Sidecars need a recycle. The incident commander escalates while pods roll.

Someone asks the question every chaos postmortem eventually surfaces:

> "Why does **aborting** an experiment take longer than **starting** one?"

Most Java chaos platforms encode blast radius as **three different artifacts**: experiment YAML in Git, environment variables on the Litmus/Chaos Mesh CRD, and a `static final int BLAST_PERCENT` in the custom fault injector nobody has touched since the last successful game day. [Kiponos.io](https://kiponos.io) collapses that into **one operational tree** — target percentage, allowed namespaces, and master kill switches — readable on every fault decision with **local `get*()` calls** and shrinkable from the dashboard while processes run.

## The problem: blast radius baked into immutable experiment config

A typical application-level fault injector gates scope like this:

```java
@Component
public class LatencyInjector implements ClientHttpRequestInterceptor {
    private static final int BLAST_PCT = 10;
    private static final int LATENCY_MS = 800;

    @Override
    public ClientHttpResponse intercept(HttpRequest req, byte[] body,
                                        ClientHttpRequestExecution exec) throws IOException {
        if (shouldInject()) {
            Thread.sleep(LATENCY_MS);
        }
        return exec.execute(req, body);
    }

    private boolean shouldInject() {
        return ThreadLocalRandom.current().nextInt(100) < BLAST_PCT;
    }
}
```

Blast policy usually lives elsewhere — scattered and static:

```yaml
# chaos-experiment-prod.yml — requires rolling restart to change
chaos:
  enabled: true
  blast_pct: 10
  target_downstream: inventory-api
  latency_ms: 800
```

Or worse — only in the chaos operator CRD, decoupled from app-layer guards:

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: inventory-latency
spec:
  mode: fixed-percent
  value: "10"
```

During a runaway experiment you need to:

1. Drop **`blast.target_pct`** to zero **immediately**
2. Flip **`chaos.master_kill`** so every injector stops regardless of pod
3. Narrow **`blast.allowed_services`** so only the rehearsal service sees faults

Doing that through Git while checkout burns budget is not resilience engineering — it is **scheduled courage without a brake pedal**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "We can abort via Chaos Mesh UI" | App-layer injectors ignore CRDs; two control planes diverge |
| "10% is safe — we calculated it" | Traffic mix shifted; 10% now hits high-value cohorts |
| "Kill switch is `kubectl delete` experiment" | Custom Java injectors keep sleeping threads |
| "Blast radius belongs in experiment Git" | Abort mid-game-day needs seconds, not merge queue |
| "Staging chaos config matches prod" | Key names drifted; rehearsal lied |

## The architecture insight

**Chaos blast radius is operational config, not experiment archaeology.** The same knobs your game-day runbook tells humans to edit — target percentage, kill switches, allowed downstreams — belong in **one live tree** the JVM already reads on every outbound call. Kiponos makes "shrink to 0%" a **dashboard edit**, not a rolling restart.

## What Kiponos.io is for chaos blast radius

[Kiponos.io](https://kiponos.io) is a real-time configuration hub. Each Spring Boot service connects **once** at startup over WebSocket; the profile tree — for example `['chaos']['platform']['prod']['live']` — loads into an **in-memory cache** inside the Java SDK.

When SRE sets `blast.target_pct` to `0` and `chaos.master_kill` to `true`, a **delta** patches only those keys. The next `kiponos.path("blast").getInt("target_pct")` on an outbound inventory call is a **local memory read** — no HTTP to a config API, no JDBC poll, no Redis round-trip on the checkout path.

`afterValueChanged` listeners let you log audit trails, increment `chaos_aborted_total`, and drain in-flight sleep timers **without** restarting the JVM.

No restart. No redeploy. No `@RefreshScope` bean recycle.

## Reference architecture

![Architecture diagram](https://litter.catbox.moe/6ceglb.png)

**Experiment YAML documents intent; the tree controls live radius.** Keep hypothesis prose in Confluence — but the **authoritative blast values** live in Kiponos where shrinking them takes seconds.

## Config tree — blast, targets, and kill switches in one place

Five folders — `chaos`, `blast`, `targets`, `guards`, `audit`:

```yaml
chaos/
  master_kill: false
  experiments_enabled: true
  fail_open_on_hub_partition: false
blast/
  target_pct: 10
  max_pct_ceiling: 25
  latency_ms: 800
  error_pct: 0
  jitter_ms: 50
targets/
  inventory-api/
    inject_latency: true
    inject_errors: false
  payments-api/
    inject_latency: false
    inject_errors: false
guards/
  allowed_services: ["checkout-api", "cart-api"]
  block_during_slo_burn: true
  slo_burn_pause_threshold: 2.5
audit/
  last_kill_by: ""
  last_kill_at_ms: 0
  last_blast_change_by: ""
```

One tree. One profile path: `['chaos']['platform']['prod']['live']`. Staging game days share **identical key layout** — only values differ.

## Java integration: fault injector + live blast gate

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LiveBlastLatencyInjector implements ClientHttpRequestInterceptor {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public LiveBlastLatencyInjector() {
        kiponos.afterValueChanged(change ->
            log.warn("Chaos config delta: path={} value={}", change.path(), change.newValue())
        );
    }

    @Override
    public ClientHttpResponse intercept(org.springframework.http.HttpRequest req, byte[] body,
                                        ClientHttpRequestExecution exec) throws IOException {
        String downstream = resolveDownstream(req);
        if (!shouldInject(downstream)) {
            return exec.execute(req, body);
        }
        sleepQuietly(kiponos.path("blast").getInt("latency_ms"));
        return exec.execute(req, body);
    }

    private boolean shouldInject(String downstream) {
        var chaos = kiponos.path("chaos");
        if (chaos.getBool("master_kill") || !chaos.getBool("experiments_enabled")) {
            return false;
        }

        var target = kiponos.path("targets", downstream);
        if (!target.getBool("inject_latency", false)) {
            return false;
        }

        int pct = kiponos.path("blast").getInt("target_pct");
        return ThreadLocalRandom.current().nextInt(100) < pct;
    }

    private String resolveDownstream(org.springframework.http.HttpRequest req) {
        return req.getURI().getHost().split("\\.")[0];
    }

    private void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms + kiponos.path("blast").getInt("jitter_ms"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

@Service
public class ChaosGuardService {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();

    public void onSloBurnSpike(double burnRate2h) {
        var guards = kiponos.path("guards");
        if (guards.getBool("block_during_slo_burn")
            && burnRate2h > guards.getFloat("slo_burn_pause_threshold")) {
            kiponos.path("chaos").set("master_kill", true);
            kiponos.path("audit").set("last_kill_by", "slo_guard_auto");
        }
    }
}
```

Every `getInt()` and `getBool()` on the outbound path is **O(1) local cache** — microseconds, not cross-region config service RTT.

Wire `ChaosGuardService` to your burn-rate adapter so experiments auto-abort when SLO debt spikes — the kill switch flips before the incident commander pages.

## Real-world scenarios

| Scenario | Without live blast tree | With Kiponos one-tree chaos policy |
|----------|-------------------------|-------------------------------------|
| Game day overshoots | Rolling restart experiment pods | Flip `chaos.master_kill: true` in dashboard |
| Shrink 10% → 2% mid-experiment | New Git commit + pipeline | Edit `blast.target_pct: 2` live |
| SLO burn during injection | Human notices Grafana | `block_during_slo_burn` auto-sets kill |
| Multi-service rehearsal | Wrong service still injected | `targets/payments-api/inject_latency: false` |
| Recovery — resume science | Second deploy wave | `master_kill: false`, restore `target_pct` |

## Performance: why fault gates must not add network I/O

- **One WebSocket per JVM** — not one config fetch per outbound HTTP call
- **Blast decision is four local bool/int reads** — nanoseconds vs inventory RTT
- **Delta patches** — zeroing `target_pct` sends one patch, not full tree reload
- **Kill switch checked before sleep** — abort takes effect on next request, not next deploy
- **No GC pressure** from re-parsing experiment YAML on every cart request

In load tests, Kiponos reads are noise on the checkout path; injected latency dominates — by design — until you shrink radius live.

## Compare to alternatives

| Approach | Mid-experiment abort speed | Hot-path read latency | Single tree for blast + kill + targets |
|----------|---------------------------|----------------------|----------------------------------------|
| Chaos Mesh CRD only | Minutes — API reconcile | N/A for app injectors | No — operator vs app split |
| Git experiment YAML | No — pipeline bound | Zero (static) but stale | Partial — no live shrink |
| Redis experiment hash | Yes with poll | Poll interval adds tail latency | Possible — custom schema discipline |
| Feature-flag SaaS | Booleans only | SDK network on evaluation | No — percentages and latency awkward |
| Env var + rolling restart | No — recycle bound | Zero after restart storm | No — scattered per deployment |
| **Kiponos SDK** | **Yes — dashboard delta** | **Zero (in-process cache)** | **Yes — one profile tree** |

## When not to use Kiponos for chaos blast radius

| Boundary | Better home |
|----------|-------------|
| Network-level partition, DNS failure, BGP blackholes | Cloud fault injection / network chaos tools |
| Kernel panic, disk fill, node drain experiments | Chaos Mesh / Litmus node faults |
| Experiment hypothesis docs and blast calculations | Confluence / game-day charter |
| Immutable cluster identity and service account ARNs | GitOps → cluster reconcile |
| Secrets for chaos webhook authentication | Vault / sealed-secrets — not live dashboard |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — add profile `['chaos']['platform']['prod']['live']` with `chaos`, `blast`, and `targets` folders matching the tree above.
2. Add `io.kiponos:sdk-boot-3` to your Spring Boot checkout service with custom fault injectors.
3. Set `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos="['chaos']['platform']['prod']['live']"`.
4. Replace `BLAST_PCT` constants with `kiponos.path("blast").getInt("target_pct")` and `master_kill` checks.
5. Register `LiveBlastLatencyInjector` on your `RestClient` / `RestTemplate` and wire `ChaosGuardService` to burn metrics.
6. Game day: in staging, set `target_pct: 5`, then flip `master_kill: true` — confirm latency injection stops **without pod restart**. Document key names in your chaos runbook.

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Config chaos multi-environment](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-config-chaos-multi-env.md)
- Related: [Circuit breaker thresholds live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-circuit-breaker-bulkhead.md)

---

*Kiponos.io — chaos teaches resilience; live blast radius gives you a brake pedal.*