---
main_image: https://litter.catbox.moe/c5dipv.jpg
title: "server.undertow.threads.io=8 Felt Permanent — We Raised IO Threads Live Under WebSocket Storm (Spring Boot)"
published: false
tags: java, springboot, performance, devops
description: Undertow IO and worker thread counts look like server.yaml forever. When WebSocket fan-out saturates IO threads, Kiponos lets you retune Undertow concurrency without recycling the JVM.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-undertow-io-threads.md
---

Tuesday 21:41. A product launch pushes **12×** normal WebSocket connections into a Spring Boot service on Undertow. CPU is only 55%. Latency is terrible. Thread dumps show **all IO threads blocked** in selector work — and `server.undertow.threads.io` is still **8**, the number someone copied from a blog post in 2021.

The platform engineer opens `application-prod.yml`:

```yaml
server:
  undertow:
    threads:
      io: 8
      worker: 64
```

PR. Review. "Is 16 safe?" Staging pipeline. By the time the deploy finishes, the launch traffic has already left for a competitor's status page.

The senior says:

> "Undertow thread model is **container architecture**. We do not change IO threads without a capacity review."

**The Aha:** IO and worker thread counts are **operational concurrency**, not sacred architecture. With [Kiponos.io](https://kiponos.io) you hold them in a live tree, apply them through a binder when safe, and keep hot-path policy reads as **local memory** — no redeploy, no `@RefreshScope` party.

Tomcat teams already learned this for `maxThreads` ([live Tomcat article](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tomcat-threads.md)). Undertow shops need the same dial.

## The hard-coded belief

Undertow separates **IO threads** (non-blocking accept/read/write) from **worker threads** (blocking servlet work). Spring Boot freezes both at boot:

```yaml
server:
  undertow:
    threads:
      io: 8
      worker: 64
    buffer-size: 1024
    direct-buffers: true
```

| What teams say | What production does |
|----------------|---------------------|
| "IO = CPU cores" | WebSocket + HTTP/2 change the shape hourly |
| "Worker count is sizing exercise" | Blocking JDBC still steals workers at 3am |
| "We'll retune after the launch" | Launch is when you needed the dial |

Wrong IO count looks like "network is slow." Wrong worker count looks like "DB is slow." Both are often **frozen concurrency**.

## What is Kiponos.io here

[Kiponos.io](https://kiponos.io) is a live operational config hub. The JVM keeps a WebSocket to the hub, merges **deltas** into an in-memory tree for profile `['realtime']['v1']['prod']['base']`, and serves `getInt()` from **local memory** on every policy check.

Git still owns "we use Undertow" and TLS wiring. The hub owns **how many IO/worker threads we tolerate tonight**.

## Architecture

![Architecture diagram](https://litter.catbox.moe/bx364h.png)

1. Connect once — snapshot loads `server_ops/undertow/*`.  
2. Ops raises `io_threads` during the storm.  
3. Delta merges async.  
4. Binder applies to the XNio worker when the runtime allows (see caveats).  
5. Filters and metrics keep reading local ints for shedding decisions.

## Config tree

```yaml
server_ops/
  undertow:
    io_threads: 8
    worker_threads: 64
    buffer_size: 1024
    direct_buffers: true
    ws_max_connections: 50000
    reject_when_workers_busy: true
  shedding:
    max_queue_ms: 200
    return_503: true
```

## Integration — Spring Boot 3 + Undertow

```java
@Configuration
public class KiponosConfig {
    @Bean(destroyMethod = "disconnect")
    public Kiponos kiponos() {
        return Kiponos.createForCurrentTeam();
    }
}
```

Policy for request shedding (always safe — pure reads):

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UndertowLoadGateFilter extends OncePerRequestFilter {

    private final Kiponos kiponos;

    public UndertowLoadGateFilter(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        var u = kiponos.path("server_ops", "undertow");
        var s = kiponos.path("server_ops", "shedding");
        if (u.getBoolean("reject_when_workers_busy", true)
                && isWorkersSaturated()
                && s.getBoolean("return_503", true)) {
            res.setStatus(503);
            res.setHeader("Retry-After", "2");
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean isWorkersSaturated() {
        // plug your Micrometer gauge / XNio busy count
        return false;
    }
}
```

Binder for live thread targets (apply on change; some options need careful Undertow lifecycle — use for **new** worker sizing and document reboot if your version cannot hot-resize IO):

```java
@Component
public class LiveUndertowBinder {

    private final Kiponos kiponos;
    private final AtomicInteger targetIo = new AtomicInteger(8);
    private final AtomicInteger targetWorker = new AtomicInteger(64);

    public LiveUndertowBinder(Kiponos kiponos) {
        this.kiponos = kiponos;
        applyFromHub();
        kiponos.afterValueChanged(ch -> {
            if (ch.path().startsWith("server_ops/undertow")) {
                applyFromHub();
            }
        });
    }

    private void applyFromHub() {
        var u = kiponos.path("server_ops", "undertow");
        targetIo.set(u.getInt("io_threads", 8));
        targetWorker.set(u.getInt("worker_threads", 64));
        // Expose targets to customizer / admin actuator that resizes XNio
        // WorkerPool when your Undertow version supports it.
    }

    public int ioThreads() { return targetIo.get(); }
    public int workerThreads() { return targetWorker.get(); }
}
```

Undertow `UndertowServletWebServerFactory` customizer at boot can seed from Kiponos so even first listen uses hub values:

```java
@Bean
public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowLive(
        LiveUndertowBinder binder) {
    return factory -> factory.addBuilderCustomizers(builder -> {
        builder.setIoThreads(binder.ioThreads());
        builder.setWorkerThreads(binder.workerThreads());
    });
}
```

Hot path:

```java
int io = kiponos.path("server_ops", "undertow").getInt("io_threads");
```

Local memory. Safe in filters.

## Honest constraint (senior-grade)

Not every Undertow version hot-swaps IO thread count after the server is fully started. **That does not kill the product story**:

1. **Shedding and caps** (`ws_max_connections`, `reject_when_workers_busy`) are live **today** with zero restart.  
2. **Target thread ints** drive the next process generation and cantrip rollouts — still seconds in the hub, not a YAML archaeology PR.  
3. Where XNio allows pool resize, binder applies immediately; where not, you still stop lying that the number is "architecture."

## Belief vs reality

| Belief | Reality |
|--------|---------|
| IO threads = core count forever | Launch fan-out is not a core-count problem |
| Only HPA fixes saturation | More pods with 8 IO each still waste capacity |
| Worker=64 is fine if CPU low | Blocking JDBC + WS can exhaust workers at 40% CPU |

## Scenarios

| Moment | Frozen reflex | Live path |
|--------|---------------|-----------|
| WS launch storm | Deploy YAML | Raise `io_threads` target + tighten shedding |
| Batch shared host | Hope | Lower `worker_threads` target for co-tenancy |
| Game day | New branch | Hub profile `gameday/live` |

Pair with [Tomcat maxThreads](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tomcat-threads.md) and [Hikari pool live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-connection-pool-live.md).

## Before / after

| Approach | Change IO/worker policy under load | Hot-path cost |
|----------|-------------------------------------|---------------|
| `application-prod.yml` | PR + rollout | Frozen |
| Full pod bounce | Minutes | N/A |
| **Kiponos** | **Dashboard delta + binder / next listen** | **Memory read** |

## Performance

- One WebSocket lifetime  
- `getInt` O(1)  
- Shedding filter must stay allocation-light  
- Do not call hub HTTP from the request thread — ever

## When not to use Kiponos

| Case | Better tool |
|------|-------------|
| Switch Netty reactive stack | Architecture migration |
| TLS cipher suite | Infra baseline |
| Node count | HPA / GitOps |

## Getting started

1. [kiponos.io](https://kiponos.io) profile `['realtime']['v1']['prod']['base']`.  
2. Move Undertow concurrency + shedding keys into `server_ops`.  
3. Ship filter for live 503 shedding first (instant value).  
4. Add binder + customizer for thread targets.  
5. Game day: fan-out WS in staging, flip shedding and targets from the dashboard.

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Undertow thread counts are operational state, not tattoos.*
