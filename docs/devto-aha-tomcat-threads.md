---
title: "Your server.tomcat.threads.max=200 Is Not Architecture — Change It Live While Traffic Runs (Spring Boot)"
published: false
tags: java, springboot, performance, devops
description: Senior teams treat Tomcat maxThreads and acceptCount as one-time YAML decisions. When Black Friday hits, that is the wrong abstraction — Kiponos lets you tune the servlet container live with zero-latency SDK reads.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tomcat-threads.md
main_image: https://files.catbox.moe/ffnuj2.jpg
---

Black Friday minute 37. P99 latency climbs. Thread dump shows **all 200 Tomcat worker threads busy** — a number someone chose in `application-prod.yml` eighteen months ago during a calm sprint planning session.

The on-call lead says what every senior Java engineer has said:

> "Tomcat thread count is **infrastructure**. We do not change that without a release."

So you open a PR. CI queues. Someone asks if 250 or 300 is "safe." The cart service keeps suffocating while you debate architecture that was never architecture — it was **operational capacity**.

Here is the moment that clicks for most staff engineers:

**`server.tomcat.threads.max` behaves like a sacred constant, but it is a dial you need during incidents.**

You can turn that dial **while the JVM keeps serving traffic** — no redeploy, no restart, no `@RefreshScope` refresh. Your app runs. You change the number remotely. The next time your code reads it, the new value is already in memory.

That is [Kiponos.io](https://kiponos.io).

## Step 1 — The hard-coded belief

Spring Boot makes it easy to bake Tomcat settings into YAML:

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 20
    accept-count: 100
```

Teams treat this like schema design. It is not. It is **runtime capacity** — like Hikari `maximumPoolSize`, like a rate limit, like a circuit breaker threshold. It should move when load moves.

| What teams say | What production does |
|----------------|---------------------|
| "We sized threads in the architecture review" | Traffic shape changes hourly |
| "Thread count needs perf lab sign-off" | Incidents do not wait for labs |
| "We'll fix it in the next release" | Users leave during the queue |

The pain is not ignorance. Senior developers **know** threads matter. They do not know there is a clean way to change them **without recycling the JVM**.

## Step 2 — The Aha: change behavior while the app runs

Wire Tomcat operational knobs into Kiponos. Your service still boots from minimal Spring config — but **live thread policy** lives in the hub:

```yaml
spring_ops/
  tomcat/
    max_threads: 200
    min_spare_threads: 20
    accept_count: 100
    connection_timeout_ms: 20000
```

During the incident, ops sets `max_threads` to `320` in the dashboard. WebSocket delivers a **delta** — only that key patches into the SDK's in-memory tree. Your binder applies it to the live connector:

```java
@Component
public class LiveTomcatBinder {

    private final Kiponos kiponos;
    private final TomcatWebServer tomcatWebServer;

    public LiveTomcatBinder(Kiponos kiponos, WebServerApplicationContext ctx) {
        this.kiponos = kiponos;
        this.tomcatWebServer = (TomcatWebServer) ctx.getWebServer();
        kiponos.afterValueChanged(this::onChange);
        applyNow();
    }

    private void onChange(ValueChange change) {
        if (change.path().startsWith("spring_ops/tomcat")) {
            applyNow();
        }
    }

    private void applyNow() {
        var t = kiponos.path("spring_ops", "tomcat");
        Connector conn = tomcatWebServer.getTomcat().getConnector();
        ProtocolHandler handler = conn.getProtocolHandler();
        if (handler instanceof AbstractProtocol<?> proto) {
            proto.setMaxThreads(t.getInt("max_threads", 200));
            proto.setMinSpareThreads(t.getInt("min_spare_threads", 20));
            proto.setAcceptCount(t.getInt("accept_count", 100));
        }
    }
}
```

**No restart.** The connector adjusts while requests flow. The same JVM that was drowning at 200 threads starts accepting more worker capacity — because you moved an operational float, not redeployed a belief.

Hot-path reads elsewhere (filters, bulkheads) stay zero-latency:

```java
int maxThreads = kiponos.path("spring_ops", "tomcat").getInt("max_threads");
```

That `getInt()` is a **local memory read** — safe in tight loops, no HTTP, no Redis RTT.

## Step 3 — How it fits together (the mechanism senior teams need)

![Architecture diagram](https://files.catbox.moe/vnnbow.png)

1. **Connect once** at startup — `Kiponos.createForCurrentTeam()`.
2. **Full tree snapshot** loads for your profile path.
3. **Dashboard edit** sends **delta only** — not a 40 KB YAML file.
4. **SDK merges async** on a WebSocket worker thread.
5. **Reads are local** — your request thread never waits on the network.

This is why the Aha lands hard: the mental model flips from "config file + restart culture" to **"operational state my process already holds."**

## Bootstrap Kiponos in Spring Boot 3

```java
@Configuration
public class KiponosConfig {

    @Bean
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

Keep **only** team id, access key, and profile path in `application.yml` — not the operational thread floats.

## Real scenarios (emotional → operational)

| Moment | Hard-coded reflex | Kiponos path |
|--------|-------------------|--------------|
| Black Friday queue | PR to raise `max` | `spring_ops/tomcat/max_threads` live |
| Downstream slow | Threads blocked waiting | Lower `accept_count` to fail fast + shed |
| Load test week | New branch per knob | Same JAR, hub profile `loadtest/live` |
| Post-incident | Debate "correct" thread count | Tune with audit trail in hub |

Pair Tomcat with [live Hikari resize](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-connection-pool-live.md) — thread exhaustion and pool exhaustion often arrive together.

## Before / after

| Approach | Change maxThreads during incident | Read cost on hot path |
|----------|-----------------------------------|------------------------|
| `application-prod.yml` | PR + deploy (25+ min) | N/A until restart |
| `@RefreshScope` + actuator | Context refresh | Bean recycle risk |
| Poll Redis for limits | Dashboard-fast | Network RTT per read |
| **Kiponos SDK** | **Dashboard delta (seconds)** | **Memory read** |

## Performance — why there is no hit

- One WebSocket per process lifetime
- `getInt()` is O(1) on cached tree
- Tomcat resize runs on `afterValueChanged`, not per request
- Virtual threads (`spring.threads.virtual.enabled=true`) still need sensible **platform thread** and accept-queue policy under burst

## When not to use Kiponos for Tomcat

| Case | Better approach |
|------|-----------------|
| Ingress replica count | Kubernetes HPA / GitOps |
| TLS cipher policy | Infrastructure baseline in Git |
| Replacing Tomcat with Netty reactive stack | Architecture migration |
| "Set threads to 50,000" without capacity planning | Load testing discipline first |

## Getting started

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['cart']['prod']['live']`.
2. Move **three** Tomcat keys out of YAML into the hub.
3. Add `LiveTomcatBinder` with `afterValueChanged`.
4. Game day: spike traffic in staging, raise `max_threads` from the dashboard, watch queue drain **without pod restart**.
5. Document boundary: Git declares wiring; hub declares **operational capacity**.

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — thread counts are not tattoos. They are live operational state.*