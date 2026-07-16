---
main_image: https://litter.catbox.moe/5316pm.jpg
title: "server.jetty.threads.max=200 Was a Tattoo — We Changed Jetty Thread Caps Live (Spring Boot)"
published: false
tags: java, springboot, performance, devops
description: Jetty max/min threads look like server configuration forever. When accept queues grow, Kiponos holds live Jetty concurrency targets and shedding policy without a redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-jetty-max-threads.md
---

Wednesday 11:02. A B2B API on Spring Boot + **Jetty** starts queueing. `QueuedThreadPool` shows **200/200** busy. The YAML still says:

```yaml
server:
  jetty:
    threads:
      max: 200
      min: 8
      idle-timeout: 60000
```

Someone wrote "architecture decision: Jetty 200" in a Confluence page. The on-call engineer treats it like schema.

> "Jetty thread pool is **container config**. Change requires release."

**The Aha:** max/min threads and idle timeout are **operational capacity**, same family as Tomcat `maxThreads` and Undertow workers. [Kiponos.io](https://kiponos.io) keeps them in a live tree; your process reads locally; you retune without recycling pods.

## Hard-coded belief

| Belief | Production |
|--------|------------|
| 200 is forever | Partner batch window spikes at 10:55 |
| Idle timeout is a tuning lab topic | Memory and FD pressure change with mobile clients |
| HPA alone fixes it | More pods × 200 still wrong under wrong shape |

```java
// Frozen culture
// server.jetty.threads.max only in application-prod.yml
```

## Kiponos for Jetty shops

WebSocket delta → in-memory tree → `getInt()` local. Profile example: `['api']['v2']['prod']['base']`.

```yaml
server_ops/
  jetty:
    max_threads: 200
    min_threads: 8
    idle_timeout_ms: 60000
    accept_queue_size: 100
  shedding:
    max_queue_ms: 250
    return_503: true
```

## Architecture

![Architecture diagram](https://litter.catbox.moe/jmmp99.png)

## Integration

```java
@Configuration
public class KiponosConfig {
    @Bean(destroyMethod = "disconnect")
    public Kiponos kiponos() {
        return Kiponos.createForCurrentTeam();
    }
}

@Component
public class LiveJettyBinder {
    private final Kiponos kiponos;
    private final AtomicInteger max = new AtomicInteger(200);
    private final AtomicInteger min = new AtomicInteger(8);

    public LiveJettyBinder(Kiponos kiponos) {
        this.kiponos = kiponos;
        pull();
        kiponos.afterValueChanged(ch -> {
            if (ch.path().startsWith("server_ops/jetty")) pull();
        });
    }

    private void pull() {
        var j = kiponos.path("server_ops", "jetty");
        max.set(j.getInt("max_threads", 200));
        min.set(j.getInt("min_threads", 8));
    }

    public int maxThreads() { return max.get(); }
    public int minThreads() { return min.get(); }
}

@Bean
public WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyLive(
        LiveJettyBinder binder) {
    return factory -> factory.addServerCustomizers(server -> {
        var tp = server.getThreadPool();
        if (tp instanceof org.eclipse.jetty.util.thread.QueuedThreadPool q) {
            q.setMaxThreads(binder.maxThreads());
            q.setMinThreads(binder.minThreads());
        }
    });
}
```

Shedding filter (always live, no Jetty internals required):

```java
if (kiponos.path("server_ops", "shedding").getBoolean("return_503", true)
        && queueWaitMs() > kiponos.path("server_ops", "shedding").getInt("max_queue_ms", 250)) {
    response.setStatus(503);
    return;
}
```

Hot path:

```java
int max = kiponos.path("server_ops", "jetty").getInt("max_threads");
```

## Scenarios

| Moment | Frozen | Live |
|--------|--------|------|
| Partner flood | Deploy | Raise `max_threads` + 503 shed |
| Night idle | Waste | Lower min / idle timeout targets |
| Game day | Branch YAML | Hub profile `gameday` |

See also [Tomcat](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tomcat-threads.md) and [Undertow](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-undertow-io-threads.md).

## Before / after

| Approach | Mid-incident change | Hot-path cost |
|----------|---------------------|---------------|
| YAML + deploy | 20+ min | Frozen |
| Pod bounce | Minutes | N/A |
| **Kiponos** | **Seconds** | **Memory** |

## When not

| Case | Prefer |
|------|--------|
| Move to Netty | Architecture |
| TLS policy | Infra GitOps |
| Replica count | HPA |

## Getting started

1. Hub profile + `server_ops/jetty` keys  
2. Binder + optional customizer  
3. Live 503 shedding first  
4. Game day without YAML PR  

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Jetty max threads are live operational state.*
