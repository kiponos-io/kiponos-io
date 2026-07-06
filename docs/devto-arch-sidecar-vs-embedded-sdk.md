---
title: "Sidecar vs Embedded Kiponos SDK — When to Colocate the Config Reader in Kubernetes (Architecture)"
published: false
tags: kubernetes, performance, architecture, java
description: Embedded SDK gives zero-hop reads inside the JVM. A Kiponos reader sidecar isolates WebSocket churn and serves polyglot pods — at a localhost latency cost. Latency, blast radius, and K8s placement patterns.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-sidecar-vs-embedded-sdk.md
main_image: https://files.catbox.moe/id94bo.jpg
cover_image: /home/moshe/work/kiponos-io/docs/devto-cover-arch-sidecar-vs-embedded-sdk.jpg
---

Thursday 09:41. Checkout pods are healthy — CPU flat, readiness green — but **p99 latency jumped 180ms** after platform rolled a new base image. The culprit was not the business container. A **config sidecar** in the same pod was reconnecting to the hub in a tight loop after a bad token rotation, and the Go checkout service was **blocking on localhost HTTP** for every `allowRequest()` call while the sidecar thrashed.

Platform lead in the war room:

> "We added the sidecar so the JVM wouldn't carry WebSocket reconnects. Now every read pays a hop."

The team had the right instinct — **isolate blast radius** — but the wrong placement for a **per-request read** on a 40k RPS path. The constant they should have questioned was not `KIPONOS_ACCESS` — it was **`reader_mode: sidecar_http_sync`** on a hot path that needed **`embedded`**.

[Kiponos.io](https://kiponos.io) always ends with **local reads** somewhere in the pod. The architecture question is **where the reader lives**: inside your application process (embedded Java or Python SDK) or in a **colocated sidecar** that holds the WebSocket and serves a localhost API. That choice drives **latency**, **failure isolation**, and **how many containers HPA counts**.

## The problem: one pod, two ways to read config

Teams default to embedded SDK in Spring Boot — and it works. Then a Rust edge proxy joins the mesh, or a legacy PHP worker needs the same live tree, and someone proposes a **shared sidecar**:

```yaml
# deployment.yaml — sidecar pattern (simplified)
spec:
  template:
    spec:
      containers:
        - name: checkout-api
          image: checkout-go:2.4.1
          env:
            - name: KIPONOS_READER_URL
              value: "http://127.0.0.1:9477"
        - name: kiponos-reader
          image: kiponos/reader-sidecar:1.2
          env:
            - name: KIPONOS_ID
              valueFrom:
                secretKeyRef:
                  name: kiponos-checkout
                  key: team-id
            - name: KIPONOS_ACCESS
              valueFrom:
                secretKeyRef:
                  name: kiponos-checkout
                  key: access-key
            - name: KIPONOS_PROFILE
              value: "['checkout']['prod']['limits']"
```

The Go service reads limits on every request:

```go
func allowRequest(tenantID string) bool {
    // Hot path — called 40k times/sec per pod
    resp, err := http.Get(fmt.Sprintf(
        "%s/v1/path/limits/%s/rpm", readerURL, tenantID))
    if err != nil {
        return false // fail closed during sidecar outage
    }
    defer resp.Body.Close()
    rpm, _ := strconv.Atoi(readBody(resp))
    return localLimiter.TryAcquire(rpm)
}
```

Meanwhile the Java payment service in the same cluster does this:

```java
// Same logical key — zero network hop
public boolean allowRequest(String tenantId) {
    int rpm = kiponos.path("limits", tenantId).getInt("rpm");
    return localLimiter(tenantId).tryAcquire(rpm);
}
```

Both pods receive the same WebSocket deltas. The Java path is **in-process cache**. The Go path adds **localhost HTTP serialization**, **sidecar CPU scheduling**, and **shared fate** when the sidecar's readiness probe fails — even if the business container is fine.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Sidecar always isolates config failures" | Sidecar crash → app cannot read; pod may stay NotReady |
| "localhost is free" | Sync HTTP per request adds 0.2–2ms p99 under load; UDS is better but not zero |
| "One sidecar image serves every language" | True for polyglot — but hot-path sync calls negate the win |
| "Embedded SDK bloats the JVM" | Typical tree is KB–low MB; WebSocket thread is lighter than a second container |
| "DaemonSet reader per node scales reads" | Node-level cache → stale views across pods; blast radius at node drain |
| "Init container can load Kiponos once" | No live deltas — you rebuilt ConfigMap with extra steps |

## The Aha

**Colocate the reader in the process that owns the hot path** when that process can host the SDK. Use a **sidecar** when you need **language neutrality**, **process isolation for reconnect churn**, or **several containers in the pod sharing one hub connection** — but prefer **async cache push** or **batch prefetch** over **per-request sync HTTP** to the sidecar.

## What Kiponos.io is in a Kubernetes pod

[Kiponos.io](https://kiponos.io) is a real-time config hub. Each reader connects once via WebSocket, receives a full tree snapshot, then applies **delta patches** into an in-memory structure. Application code calls `getInt()`, `getBool()`, `get()` — those are **reads against local memory**, not hub round-trips.

Profile path for this story:

```
['checkout']['prod']['limits']
```

**Embedded SDK:** the Java or Python library runs inside your app JVM or interpreter. `kiponos.path("limits", tenantId).getInt("rpm")` is nanoseconds-to-microseconds — no socket leave the process.

**Reader sidecar:** a small container in the **same pod network namespace** holds the WebSocket. Siblings reach it at `127.0.0.1` via HTTP, gRPC, or a Unix domain socket. Updates still arrive as deltas; the sidecar's cache is warm. Your app chooses **how often** it crosses the boundary.

No restart. No redeploy. No ConfigMap reload. The hub pushes; the reader — embedded or sidecar — applies.

## Architecture: embedded vs sidecar in one pod

![Architecture diagram](https://files.catbox.moe/9dts5e.png)

**HPA sees one pod** in both cases. Embedded: one container. Sidecar: two containers, **one shared network namespace** — `127.0.0.1` is valid. Do not confuse this with a **cluster-wide config service**; per-pod cache is the scale pattern ([SDK per pod](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-sdk-per-pod.md)).

## Config tree

```yaml
checkout/
  prod/
    limits/
      default_rpm: 1200
      fail_closed_on_reader_error: true
      reader_mode: embedded          # embedded | sidecar_push | sidecar_sync
      sidecar/
        listen_uds: /var/run/kiponos/reader.sock
        prefetch_paths:
          - limits/*
        push_batch_ms: 50
      tenant_acme/
        rpm: 800
        burst: 120
      tenant_globex/
        rpm: 2400
        burst: 400
      degraded/
        global_rpm_cap: 400
        enabled: false
```

`reader_mode` is **operational documentation** in the tree — ops and SRE agree per deployment which pattern the pod uses. Changing `degraded/global_rpm_cap` still fans out via WebSocket to every connected reader.

## Integration pattern 1: embedded SDK (default for Java)

```java
@Configuration
public class KiponosCheckoutConfig {

    @Bean(destroyMethod = "disconnect")
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
@Service
public class CheckoutRateLimiter {

    private final Kiponos kiponos;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public CheckoutRateLimiter(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("limits/")) {
                limiters.clear(); // cheap rebuild on delta
            }
        });
    }

    public boolean allowRequest(String tenantId) {
        var limits = kiponos.path("limits", tenantId);
        if (kiponos.path("limits", "degraded").getBool("enabled", false)) {
            int cap = kiponos.path("limits", "degraded").getInt("global_rpm_cap", 400);
            return globalLimiter().tryAcquire(cap);
        }
        int rpm = limits.getInt("rpm",
                kiponos.path("limits").getInt("default_rpm", 1200));
        return limiterFor(tenantId).tryAcquire(rpm);
    }
}
```

**When to choose embedded:** Spring Boot, Micronaut, or any JVM service where `io.kiponos:sdk-boot-3` runs in the same process as the hot path. This is the **lowest latency** and **simplest ops** — one container, one readiness probe, one memory profile.

## Integration pattern 2: sidecar with push cache (polyglot)

Avoid per-request HTTP. Run the sidecar with **prefetch + push** into a shared volume or UDS notification:

```yaml
# kiponos-reader sidecar env
KIPONOS_PROFILE: "['checkout']['prod']['limits']"
KIPONOS_LISTEN_UDS: /var/run/kiponos/reader.sock
KIPONOS_PUSH_CACHE_DIR: /var/run/kiponos/cache
KIPONOS_PREFETCH: "limits/*"
```

Go worker watches the cache file the sidecar updates on each delta:

```go
// Hot path — mmap or atomic snapshot read, no HTTP per request
func (r *RuntimeLimits) RPM(tenantID string) int {
    snap := r.cache.Load() // updated by sidecar push + inotify
    if snap.Degraded.Enabled {
        return snap.Degraded.GlobalRpmCap
    }
    if t, ok := snap.Tenants[tenantID]; ok {
        return t.Rpm
    }
    return snap.DefaultRpm
}
```

**When to choose sidecar:** non-JVM primary container, **multiple processes** in the pod sharing one hub connection, or deliberate **isolation** so WebSocket reconnect threads never share the app's thread pool. Pay the colocation cost **once per delta**, not **once per request**.

## Real-world scenarios

| Event | Embedded SDK | Sidecar reader |
|-------|--------------|----------------|
| Ops lowers `tenant_globex/rpm` during abuse | Next `getInt()` in JVM — sub-ms | Push updates snapshot; Go reads local file |
| Bad `KIPONOS_ACCESS` rotation | App logs handshake failure; single container restart | Sidecar CrashLoopBackOff; app fails closed on stale cache |
| HPA adds pod at peak | New JVM connects; snapshot + deltas | Sidecar + app both start; share startup ordering |
| JVM GC pause storm | SDK read unaffected — same heap | App unaffected; sidecar may miss push tick — bounded staleness |
| Mesh mTLS rollout | One container to drain | **Two** containers must pass preStop — coordinate with [graceful shutdown](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-graceful-shutdown.md) |
| Platform adds Rust + Java in same pod | Two SDK connections (wasteful) | **One sidecar**, two consumers — saves hub connections |

## Performance: latency and blast radius

- **Embedded `getInt()`** — in-process tree lookup; dominated by business logic, not config. At 40k RPS, no per-read syscall leave the JVM.
- **Sidecar sync HTTP per request** — adds **0.2–2ms p99** on localhost under CPU contention; avoid on authorization and rate-limit paths.
- **Sidecar UDS + push snapshot** — hot path reads **local memory or mmap**; delta cost amortized across all requests until next change.
- **Blast radius embedded** — SDK bug or reconnect storm shares the app process; use resource limits and circuit breakers in `afterValueChanged` listeners.
- **Blast radius sidecar** — hub token failure isolates to sidecar container; app may serve **last known** limits if `fail_closed_on_reader_error` is false — explicit policy choice.
- **Hub connections** — one WebSocket per reader instance; sidecar deduplicates when multiple language runtimes share a pod.
- **HPA memory** — sidecar adds **32–128Mi** baseline per pod; embedded tree is usually **negligible vs heap**.

Rule of thumb: if config is read **more than once per request** or on **every request**, embed. If config is read **at startup and on notification**, sidecar push is viable.

## Compare to alternatives

| Approach | Hot-path latency | Polyglot | Blast radius | K8s complexity |
|----------|------------------|----------|--------------|----------------|
| ConfigMap volume mount | Zero (static) | Yes | Pod restart to update | Low |
| Central Redis per read | 1–5ms RTT | Yes | Redis outage hits all | Medium |
| Spring Cloud Config poll | 10–100ms+ | Java-centric | Config server herd | Medium |
| DaemonSet node agent | Zero after local copy | Yes | **Node drain** affects many pods | High — stale risk |
| **Embedded Kiponos SDK** | **~0 (memory)** | Java/Python | Shared with app process | **Low — one container** |
| **Kiponos reader sidecar** | **0 with push; ms with sync HTTP** | **Yes** | **Isolated container** | **Medium — two containers** |

## When not to use each pattern

| Case | Better approach |
|------|-----------------|
| Pure Java Spring Boot hot path | **Embedded SDK** — sidecar adds latency for no gain |
| Per-request sync HTTP to sidecar at >10k RPS | Embed or push-cache; measure p99 before keeping sidecar |
| Secrets (`KIPONOS_ACCESS`, DB passwords) | Kubernetes Secrets + rotation; not in config tree |
| Cluster-wide identical static bootstrap | GitOps ConfigMap for skeleton; Kiponos for operational deltas |
| Strong guarantee all pods read identical value at same instant | Version keys in business logic; Kiponos is eventual per pod |
| Config only changes on deploy | GitOps alone — live hub is unnecessary complexity |

## Kubernetes placement patterns (quick reference)

| Pattern | Use when |
|---------|----------|
| **Single container + embedded SDK** | Java/Python microservice; default choice |
| **Sidecar in same pod** | Polyglot, legacy binary, or shared hub connection |
| **Sidecar + init ordering** | App must wait for sidecar snapshot — use `depends_on` readiness, not init-only |
| **DaemonSet reader** | Generally **avoid** for Kiponos — breaks per-pod snapshot semantics |
| **Separate Deployment "config service"** | Remote network hop — defeats local read; use only for admin tools |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['checkout']['prod']['limits']`.
2. **Java path:** add `io.kiponos:sdk-boot-3`, set `KIPONOS_ID`, `KIPONOS_ACCESS`, `-Dkiponos="['checkout']['prod']['limits']"`.
3. Wire `CheckoutRateLimiter` with embedded `getInt()` — load test at expected QPS; confirm config reads are invisible in traces.
4. **Sidecar path (if needed):** add `kiponos-reader` container to the pod spec; mount UDS volume; configure `KIPONOS_PREFETCH=limits/*`.
5. Replace per-request HTTP with **push snapshot** reads in the app; re-run load test — p99 should match embedded within noise.
6. Game day: revoke access token — observe embedded vs sidecar failure modes; tune `fail_closed_on_reader_error`.

## Further reading

- [Developer Quickstart](https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [One Kiponos SDK per pod](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-sdk-per-pod.md)
- Related: [K8s config without pod restart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md)

---

*Kiponos.io — embed the reader on the hot path; sidecar the hub connection when the process boundary demands it — never pay per-request localhost tax for live limits.*