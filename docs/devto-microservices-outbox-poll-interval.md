---
title: "Tune Transactional Outbox Poll Intervals Live (Kiponos Java SDK)"
published: true
tags: java, microservices, eventdriven, realtime
description: Outbox relay workers hammer the database when poll_ms is frozen in YAML. Kiponos feeds live poll cadence and batch size to every Java relay JVM — zero-latency reads on each scheduler tick.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-outbox-poll-interval.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-microservices-outbox.jpg
---

Order placement writes to `orders` and `outbox_events` in one transaction. A separate **outbox relay** polls unpublished rows, publishes to Kafka, marks them sent. During month-end close, the outbox table grows to six million rows — and your relay still wakes every **200 milliseconds** because `outbox.poll_interval_ms` shipped in `application-prod.yml` three quarters ago.

The platform lead on bridge:

> "Relay CPU is fine. **Postgres is not.** Every pod is doing `SELECT … FOR UPDATE SKIP LOCKED` five times per second."

Relay poll interval is not architecture. It is **how aggressively you drain backlog without asphyxiating the primary**. Static YAML cannot slow down when the database is red and speed up when the queue is deep.

## Why outbox relays break with static poll config

Typical Spring `@Scheduled` relay:

```java
@Scheduled(fixedDelayString = "${outbox.poll_interval_ms:200}")
public void relayPendingEvents() {
    List<OutboxRow> batch = outboxRepo.claimBatch(100);
    batch.forEach(this::publishAndMarkSent);
}
```

That `200` and batch `100` usually come from:

1. **Per-service YAML** — order service polls at 200ms, notification relay at 500ms; nobody aligns during an incident
2. **Helm values** — changing cadence means rolling eight relay deployments while the outbox grows
3. **Shared config table** — polling the config table before every outbox poll adds latency and another failure mode

Outbox relay ticks are **high-frequency reads** on a hot scheduler thread. You need local memory lookups and async updates — the same contract as [live saga step timeouts](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-saga.md).

## What teams believe

| What teams say | What production does |
|----------------|---------------------|
| "Faster polling means lower end-to-end latency" | Faster polling **multiplies row locks** on a hot outbox table |
| "200ms is conservative" | Twenty relay pods × 5 polls/sec = **100 claims/sec** on one table |
| "We'll scale relay replicas" | More replicas **increase** DB contention without tuning interval |
| "Batch size is separate from poll interval" | Large batches + fast polls = **long transactions** blocking writers |

## The Aha

**`outbox.poll_interval_ms` feels like infrastructure bootstrap, but poll cadence is operational load management** — stretch to 2s when Postgres is degraded, drop to 50ms when clearing a Black Friday backlog. [Kiponos.io](https://kiponos.io) feeds `poll_interval_ms` and `claim_batch_size` with local `getLong()` on every relay tick — no redeploy, no `@RefreshScope` refresh storm.

## What is Kiponos.io (for outbox relays)

[Kiponos.io](https://kiponos.io) holds relay operational policy under profile `['orders']['v2']['prod']['outbox']` → `relay/orders`. WebSocket deltas update `poll_interval_ms`, `claim_batch_size`, and `pause_relay` in every connected relay JVM.

On each scheduler tick, `kiponos.path("relay", "orders").getLong("poll_interval_ms")` is a **local memory read** — no HTTP round trip to a config server while the outbox table is already under lock pressure. Ops flips `degrade_during_db_incident: true`; within one tick window, relays stretch interval fleet-wide.

## Architecture: one tree, many relay workers

![Architecture diagram](https://litter.catbox.moe/vladi9.png)

Every relay connects to profile `['orders']['v2']['prod']['outbox']`. When NOC raises `poll_interval_ms` during a DB incident, **all relay JVMs** see the new cadence on the next tick — no config server poll, no inter-service "what is interval now?" REST calls.

## Outbox relay config tree

```yaml
relay/
  orders/
    poll_interval_ms: 200
    claim_batch_size: 100
    max_publish_retries: 3
    pause_relay: false
    degrade_during_db_incident: false
    incident_poll_interval_ms: 2000
  notifications/
    poll_interval_ms: 500
    claim_batch_size: 50
    pause_relay: false
  global/
    alert_on_lag_rows: 50000
    lag_alert_webhook: ops-oncall
```

Platform ops edits **one folder**; order and notification relays each read **their** subtree locally.

## Java integration (outbox relay)

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderOutboxRelay {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final OutboxRepository outboxRepo;
    private Instant lastRun = Instant.EPOCH;

    public OrderOutboxRelay(OutboxRepository outboxRepo) {
        this.outboxRepo = outboxRepo;
    }

    @Scheduled(fixedDelay = 50)
    public void tick() {
        var cfg = kiponos.path("relay", "orders");
        if (cfg.getBool("pause_relay")) {
            return;
        }
        long intervalMs = cfg.getBool("degrade_during_db_incident")
            ? cfg.getLong("incident_poll_interval_ms", 2000)
            : cfg.getLong("poll_interval_ms", 200);
        if (Duration.between(lastRun, Instant.now()).toMillis() < intervalMs) {
            return;
        }
        lastRun = Instant.now();
        int batch = cfg.getInt("claim_batch_size", 100);
        outboxRepo.claimAndPublish(batch, cfg.getInt("max_publish_retries", 3));
    }
}
```

`getLong()` is a **local cache lookup** — safe inside the relay scheduler hot path.

Optional audit when ops changes cadence mid-incident:

```java
kiponos.afterValueChanged(change ->
    log.warn("Outbox relay config changed: {} → {}", change.path(), change.newValue())
);
```

## Real-world scenarios

| Scenario | Without Kiponos | With Kiponos |
|----------|-----------------|--------------|
| Postgres CPU pegged | Emergency Helm values + relay rollouts | Set `degrade_during_db_incident: true`, bump `incident_poll_interval_ms` |
| Black Friday backlog | Wait for deploy to lower interval | Drop `poll_interval_ms` to 50 live |
| Kafka broker outage | Manually scale relays to zero | Flip `pause_relay: true` in dashboard |
| Poison publish loop | Edit YAML, restart fleet | Lower `claim_batch_size` and `max_publish_retries` during triage |

## Performance

- **One WebSocket** per relay JVM — not a config fetch per outbox tick
- **Reads are O(1)** on the SDK cache — microseconds per scheduler decision
- **Delta patches** — changing poll interval does not reload the full relay tree
- **No DB poll** for config on the relay hot path — config reads do not compete with `SKIP LOCKED` claims

## Compare to alternatives

| Approach | Cross-relay consistency | Mid-incident change | Read latency |
|----------|-------------------------|---------------------|--------------|
| Per-service YAML | Drift guaranteed | Rolling restart fleet | Zero after restart |
| Central DB config | Possible | DB round-trip per read | Milliseconds |
| Redis pub/sub | Custom glue | Invalidation complexity | Cache RTT |
| **Kiponos shared tree** | **Single source of truth** | **Dashboard edit** | **Zero (local)** |

## When not to use Kiponos

| Situation | Better approach |
|-----------|-----------------|
| Exactly-once broker semantics | Broker idempotency keys and dedup store — not poll tuning |
| Outbox schema migration | Flyway/Liquibase deploy — structural change |
| Per-tenant relay isolation | Separate relay deployments per tenant |
| Sub-millisecond publish SLA | Dedicated streaming pipeline — outbox is eventual |

## Getting started (15 minutes)

1. [Free TeamPro at kiponos.io](https://kiponos.io) — one profile for `relay/orders/*`
2. Add `io.kiponos:sdk-boot-3` to each outbox relay service
3. Wire `KIPONOS_ID`, `KIPONOS_ACCESS`, and `-Dkiponos=...` on every relay pod
4. Replace `${outbox.poll_interval_ms}` with `kiponos.path("relay", "orders").getLong("poll_interval_ms")`
5. Seed a backlog in staging; flip `degrade_during_db_incident` in dashboard; confirm claim rate drops without pod restart

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [Event bus routing live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-microservices-event-routing.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

## What is next

Outbox relays pair with **saga retry policy** and **event topic routing** in the same live tree — drain backlog gently while orchestration and routing stay tunable mid-flight.

---

*Kiponos.io — real-time config for Java. Poll the outbox without polling a config server.*