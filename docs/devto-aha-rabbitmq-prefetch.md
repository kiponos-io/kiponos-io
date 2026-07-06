---
title: "spring.rabbitmq.listener.simple.prefetch=250 Was Broker Gospel — We Cut It Live During the Queue Flood (Spring AMQP)"
published: false
tags: java, rabbitmq, springboot, devops
description: RabbitMQ prefetch feels like broker tuning set once in Spring YAML. When consumer memory spikes and ack lag grows, prefetch is operational — Kiponos feeds live listener policy without pod restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-rabbitmq-prefetch.md
main_image: https://files.catbox.moe/e166i8.jpg
---

Notification dispatch minute 14. RabbitMQ management UI shows **47,000 unacked messages** pinned to twelve consumers — each pod prefetched **250 messages** because `spring.rabbitmq.listener.simple.prefetch=250` shipped in the platform starter three years ago when throughput labs looked great on synthetic load.

Heap graphs on the consumer fleet climb together. GC pause times breach 400ms. The messaging lead says what every senior platform engineer has memorized:

> "Prefetch is **broker configuration**. You do not change it without a release and a rebalance plan."

But the queue depth is not waiting for your release train. Prefetch is not architecture — it is **how many messages each consumer hoards before finishing work**. When handlers slow down, high prefetch turns every pod into a warehouse of unacked inventory.

Here is the moment that lands for most staff engineers:

**`prefetch` behaves like sacred broker tuning, but it is a dial you need when consumer lag and memory move together.**

You can turn that dial **while the JVM keeps consuming** — no redeploy, no listener container recycle, no `@RefreshScope` refresh. Your app runs. You change the number remotely. The next time your binder applies it, the live `SimpleMessageListenerContainer` already holds the new prefetch.

That is [Kiponos.io](https://kiponos.io).

## Step 1 — The hard-coded belief

Spring Boot makes it easy to bake Rabbit listener settings into YAML:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 250
        concurrency: 4
        max-concurrency: 12
        acknowledge-mode: manual
```

Teams treat this like messaging infrastructure design. It is not. It is **runtime backpressure** — like Kafka `max.poll.records`, like an HTTP client timeout, like a bulkhead limit. It should move when handler latency moves.

Your notification handler assumes unbounded batches per channel:

```java
@RabbitListener(queues = "notifications.dispatch")
public void onDispatch(NotificationPayload payload) {
    templateRenderer.render(payload);
    pushGateway.send(payload);
    auditLog.record(payload);
}
```

When `templateRenderer` hits a slow downstream font service, each consumer still holds 250 prefetched messages **in memory and unacked**. Problems compound:

1. **Memory pressure** — prefetch × message size × pod count
2. **Fairness collapse** — fast consumers finish; slow ones hoard
3. **Restart to fix** — rolling deploy recreates containers but rebalances pain

| What teams say | What production does |
|----------------|---------------------|
| "We sized prefetch in the architecture review" | Handler p99 changes hourly during incidents |
| "Lower prefetch means more broker round-trips" | Unacked hoarding hurts more than extra `basic.qos` |
| "Fix the slow renderer, don't touch prefetch" | Both happen; shrinking prefetch buys breathing room |
| "Listener config lives in Git" | Queue floods do not wait for merge queues |

The pain is not ignorance. Senior developers **know** prefetch matters. They do not know there is a clean way to change it **without recycling listener containers**.

## Step 2 — The Aha: change behavior while the app runs

Wire Rabbit operational knobs into Kiponos. Your service still boots from minimal Spring config — but **live listener policy** lives in the hub:

```yaml
rabbit/
  listener/
    notifications/
      prefetch: 250
      concurrency: 4
      max_concurrency: 12
      enabled: true
    billing_events/
      prefetch: 100
      enabled: true
  ops/
    flood_mode: false
    flood_prefetch: 25
    pause_new_consumers: false
```

During the incident, ops sets `flood_prefetch` to `25` and enables `flood_mode` in the dashboard. WebSocket delivers a **delta** — only those keys patch into the SDK's in-memory tree. Your binder applies it to the live `SimpleMessageListenerContainer`:

```java
@Component
public class LiveRabbitPrefetchBinder {

    private final Kiponos kiponos;
    private final SimpleRabbitListenerContainerFactory containerFactory;
    private final RabbitListenerEndpointRegistry registry;

    public LiveRabbitPrefetchBinder(
            Kiponos kiponos,
            SimpleRabbitListenerContainerFactory containerFactory,
            RabbitListenerEndpointRegistry registry) {
        this.kiponos = kiponos;
        this.containerFactory = containerFactory;
        this.registry = registry;
        kiponos.afterValueChanged(this::onChange);
        applyNow();
    }

    private void onChange(ValueChange change) {
        if (change.path().startsWith("rabbit/listener")
                || change.path().startsWith("rabbit/ops")) {
            applyNow();
        }
    }

    private void applyNow() {
        int prefetch = resolvePrefetch("notifications");
        containerFactory.setPrefetchCount(prefetch);
        registry.getListenerContainers().forEach(container -> {
            if (container instanceof SimpleMessageListenerContainer smlc) {
                smlc.setPrefetchCount(prefetch);
            }
        });
        log.warn("Rabbit prefetch applied: {}", prefetch);
    }

    private int resolvePrefetch(String queueKey) {
        if (kiponos.path("rabbit", "ops").getBool("flood_mode", false)) {
            return kiponos.path("rabbit", "ops").getInt("flood_prefetch", 25);
        }
        return kiponos.path("rabbit", "listener", queueKey).getInt("prefetch", 250);
    }
}
```

**No restart.** Containers adjust while messages flow. The same JVM that was hoarding 250 unacked messages per channel starts pulling smaller slices — because you moved an operational float, not redeployed a belief.

Hot-path reads elsewhere (metrics, adaptive batching) stay zero-latency:

```java
int prefetch = kiponos.path("rabbit", "listener", "notifications").getInt("prefetch");
```

That `getInt()` is a **local memory read** — safe in tight loops, no HTTP, no Redis RTT.

## Step 3 — How it fits together (the mechanism senior teams need)

![Architecture diagram](https://files.catbox.moe/exjzv9.png)

1. **Connect once** at startup — `Kiponos.createForCurrentTeam()`.
2. **Full tree snapshot** loads for your profile path.
3. **Dashboard edit** sends **delta only** — not a 40 KB YAML file.
4. **SDK merges async** on a WebSocket worker thread.
5. **Reads are local** — your listener thread never waits on the network for policy.

This is why the Aha lands hard: the mental model flips from "broker YAML + restart culture" to **"operational state my process already holds."**

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

Keep **only** team id, access key, and profile path in `application.yml` — not the operational prefetch floats.

## Real scenarios (emotional → operational)

| Moment | Hard-coded reflex | Kiponos path |
|--------|-------------------|--------------|
| Renderer dependency brownout | PR to lower prefetch | `rabbit/ops/flood_mode` live |
| Black Friday email burst | Same 250 prefetch per pod | Hub profile `peak/conservative` |
| Poison message hunt | Stop all consumers manually | `enabled: false` on one listener key |
| Post-incident recovery | Debate "correct" prefetch forever | Disable `flood_mode`, audit trail in hub |

Pair prefetch with [live Hikari resize](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-hikari-pool-max.md) — slow handlers often mean DB pressure and message hoarding arrive together.

## Before / after

| Approach | Change prefetch during queue flood | Read cost on hot path |
|----------|-----------------------------------|------------------------|
| `application-prod.yml` | PR + deploy (25+ min) | N/A until restart |
| `@RefreshScope` + actuator | Context refresh | Bean recycle risk |
| Poll Redis for limits | Dashboard-fast | Network RTT per read |
| **Kiponos SDK** | **Dashboard delta (seconds)** | **Memory read** |

## Performance — why there is no hit

- One WebSocket per process lifetime
- `getInt()` is O(1) on cached tree
- Prefetch resize runs on `afterValueChanged`, not per message
- Smaller prefetch reduces unacked memory — often **improves** GC under lag
- `setPrefetchCount` on live containers avoids full listener factory rebuild

## When not to use Kiponos for Rabbit prefetch

| Case | Better approach |
|------|-----------------|
| Broker cluster sizing and queue TTL policy | Infrastructure GitOps |
| TLS and vhost credentials | Vault + Git |
| Migrating from RabbitMQ to Kafka | Architecture migration |
| Setting prefetch to 1 on every queue without measurement | Load testing discipline first |

## Getting started

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['notifications']['prod']['rabbit']`.
2. Move **prefetch** and **concurrency** keys out of YAML into the hub.
3. Add `LiveRabbitPrefetchBinder` with `afterValueChanged`.
4. Game day: inject slow handler in staging, enable `flood_mode`, watch unacked count drain **without pod restart**.
5. Document boundary: Git declares wiring; hub declares **operational backpressure**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — prefetch is how much you hoard per bite, not broker scripture from 2022.*