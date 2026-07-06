---
title: "Edge Worker CPU Caps You Can Lower Before the Node Melts (Kiponos Java SDK)"
published: false
tags: java, kubernetes, edge, performance
description: Edge transform pods hard-code thread pools and CPU throttle percentages in JVM args. When regional traffic spikes overload edge nodes, CPU policy is operational — Kiponos feeds live caps to Java edge workers on Kubernetes.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-edge-worker-cpu-cap.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-k8s-edge-worker-cpu-cap.jpg
---

Saturday 11:05 UTC. APAC edge region `ap-southeast-2` sees a viral campaign clip. Your **edge-transform** Deployment — image resize, signed URL rewrite, lightweight auth — runs on **regional K8s edge nodes** with `limits.cpu: "2"` per pod. Node CPU pegs at **98%** across the pool. Latency for cache misses spikes; origin shielding fails because workers spend all cycles on transforms instead of shedding load.

Helm sets `-Dworker.threads=32` and a static `CPU_THROTTLE_PERCENT=90` env var. Platform wants to drop to **60%** effective CPU and **16 threads** until the campaign cools — but that means a **new image tag** or ConfigMap Reloader restart on **every edge node** while nodes are already hot.

Edge SRE in the bridge:

> "We need to **throttle locally** before kubelet OOMs the pool. We cannot wait for a US-hours deploy."

Edge workers are not core DC services — they are **regional shock absorbers**. Their CPU and thread policy should move **faster than GitOps from headquarters**.

**The Aha:** Java edge pods read `edge/workers/cpu_cap_percent` and `max_transform_threads` from [Kiponos.io](https://kiponos.io). Ops lowers caps for profile `['edge']['apac']['transform']` in the dashboard; every connected worker applies new limits on the next admission check — **no rolling restart across the edge fleet**.

## The problem — static CPU policy on edge pods

```yaml
# helm/edge-transform/values-apac.yaml
resources:
  limits:
    cpu: "2"
    memory: 1Gi
env:
  - name: WORKER_THREADS
    value: "32"
  - name: CPU_THROTTLE_PERCENT
    value: "90"
```

```java
public class TransformWorker {
    private static final int THREADS =
            Integer.parseInt(System.getenv().getOrDefault("WORKER_THREADS", "32"));
    private final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

    public void submit(TransformJob job) {
        pool.submit(() -> runTransform(job));
    }
}
```

| What teams believe | What production does |
|--------------------|---------------------|
| "K8s CPU limit enforces cap" | Limit causes **throttling**, not graceful shed |
| "HPA adds edge pods" | Node pool is **fixed** at edge; no time to provision |
| "CDN will absorb it" | Miss traffic hits your transform fleet |
| "Edge config rarely changes" | Viral events change it **hourly** |

## What Kiponos.io is — for edge worker policy

[Kiponos.io](https://kiponos.io) delivers **regional operational policy** to each edge pod via WebSocket deltas. Profile path per region:

```
['edge']['apac']['transform']
```

`kiponos.path("edge", "workers").getInt("cpu_cap_percent")` on the transform admission path is a **local read** — critical when edge pods handle **thousands of requests per second** and cannot poll a central config store.

Same JVM that runs transforms holds the SDK — no sidecar hop on localhost ([embedded SDK](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-sidecar-vs-embedded-sdk.md)).

## Architecture

![Architecture diagram](https://litter.catbox.moe/wyki9q.png)

## Config tree

```yaml
edge/
  workers/
    cpu_cap_percent: 90
    max_transform_threads: 32
    min_transform_threads: 4
    shed_load_above_cpu_percent: 95
    emergency_throttle_mode: false
    emergency_cpu_cap_percent: 60
    emergency_max_threads: 16
  transform/
    max_input_bytes: 8388608
    max_output_dimension_px: 4096
    jpeg_quality: 82
    timeout_ms: 2500
  regional/
    region_code: ap-southeast-2
    prefer_shed_over_queue: true
    origin_rps_budget: 800
  health/
    report_cpu_to_hub: false
    heartbeat_interval_seconds: 30
```

## Integration — EdgeTransformService with live caps

```java
@Configuration
public class KiponosEdgeConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey) {
        return Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath("['edge']['apac']['transform']")
                .build();
    }
}
```

```java
@Component
public class EdgeCpuGate {

    private final Kiponos kiponos;
    private final OperatingSystemMXBean osBean =
            ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    public EdgeCpuGate(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    public boolean admit() {
        var workers = kiponos.path("edge", "workers");
        int cap = effectiveCpuCap();
        double load = osBean.getSystemLoadAverage();
        int cpus = Runtime.getRuntime().availableProcessors();
        double cpuPct = (load / Math.max(1, cpus)) * 100.0;
        if (cpuPct > workers.getInt("shed_load_above_cpu_percent", 95)) {
            return false;
        }
        return cpuPct <= cap;
    }

    private int effectiveCpuCap() {
        var workers = kiponos.path("edge", "workers");
        if (workers.getBool("emergency_throttle_mode", false)) {
            return workers.getInt("emergency_cpu_cap_percent", 60);
        }
        return workers.getInt("cpu_cap_percent", 90);
    }
}
```

```java
@Service
public class EdgeTransformService {

    private final Kiponos kiponos;
    private final EdgeCpuGate cpuGate;
    private volatile ExecutorService pool;

    public EdgeTransformService(Kiponos kiponos, EdgeCpuGate cpuGate) {
        this.kiponos = kiponos;
        this.cpuGate = cpuGate;
        this.pool = rebuildPool();
        kiponos.afterValueChanged(c -> {
            if (c.path().startsWith("edge/workers")) {
                pool.shutdown();
                pool = rebuildPool();
            }
        });
    }

    private ExecutorService rebuildPool() {
        var w = kiponos.path("edge", "workers");
        int threads = w.getBool("emergency_throttle_mode", false)
                ? w.getInt("emergency_max_threads", 16)
                : w.getInt("max_transform_threads", 32);
        int min = w.getInt("min_transform_threads", 4);
        int effective = Math.max(min, threads);
        return Executors.newFixedThreadPool(effective);
    }

    public TransformResult transform(TransformRequest req) {
        if (!cpuGate.admit()) {
            return TransformResult.shed("edge_cpu_pressure");
        }
        int timeout = kiponos.path("edge", "transform").getInt("timeout_ms", 2500);
        int maxBytes = kiponos.path("edge", "transform").getInt("max_input_bytes", 8_388_608);
        if (req.bytes().length > maxBytes) {
            return TransformResult.reject("payload_too_large");
        }
        return runWithTimeout(req, timeout);
    }
}
```

Ops enables `emergency_throttle_mode: true` → CPU cap **60%**, threads **16** → regional pool stops melting — **same container image**, same `limits.cpu: "2"`.

## Real scenarios

| Event | Static threads=32, cap=90% | Kiponos edge policy |
|-------|---------------------------|---------------------|
| Viral clip APAC | Node CPU 98%, widespread 503 | `emergency_throttle_mode` live |
| Campaign ends | Still throttled until deploy | Disable emergency mode |
| Origin protection | Transform starves shield budget | Lower `origin_rps_budget` key |
| EU region quiet | Same policy as APAC | Profile `['edge']['eu']['transform']` |
| New edge pod | Cold start threads from env | Snapshot + deltas on connect |

## Performance

- Admission `admit()`: one local `getInt` + MXBean read — **microseconds**
- One WebSocket per edge pod — not a config fetch per transform
- Pool resize on **delta only** — not per request
- Shed path returns fast — protects node without waiting for HPA
- Pair with [live config without restart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md) during regional incidents

## Compare to alternatives

| Approach | APAC viral throttle in minutes | Per-region policy |
|----------|-------------------------------|-------------------|
| Helm per-region values | US-hours PR cycle | Yes but slow |
| ConfigMap + Reloader | Rolling restart on hot nodes | Poor timing |
| CPU limit only | Throttle + latency tail | No graceful shed |
| Central Redis policy | RTT from edge | Bad on hot path |
| **Kiponos embedded SDK** | **Dashboard seconds** | **Profile per region** |

## When not to use Kiponos for edge CPU

| Case | Better approach |
|------|-----------------|
| Node pool size / instance type | Terraform / cluster autoscaler |
| CDN cache TTL and purge | CDN control plane API |
| TLS cert on edge ingress | cert-manager |
| Replacing self-hosted edge with managed CDN | Architecture migration |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['edge']['apac']['transform']`.
2. Add `KIPONOS_ID` / `KIPONOS_ACCESS` to edge Deployment ([secrets boundary](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-secrets-vs-config-boundary.md)).
3. Create `edge/workers` with `cpu_cap_percent` and `max_transform_threads`.
4. Wire `EdgeCpuGate.admit()` before transform execution.
5. Game day: load-test staging edge pool, flip `emergency_throttle_mode`, confirm shed rate rises **without** rollout.
6. Document: Git owns **node pool**; hub owns **regional throttle**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [CDN edge rules](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-cdn-edge-rules.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — edge workers throttle on today's regional heat, not launch-day Helm.*