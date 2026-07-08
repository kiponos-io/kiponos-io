---
title: "Stream Replay Rate Limits — Cap Catch-Up Without Job Restart (Java SDK)"
published: false
tags: java, kafka, data, sre
description: Replay throttle constants risk overwhelming sinks. Kiponos caps replay RPS live during recovery.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-stream-replay-rate-limits.md
main_image: https://files.catbox.moe/e166i8.jpg
---

Monday 09:14 UTC. **inventory-deltas** consumer crashed for forty minutes — offset lag now **2.1 million events**. On restart, the replay worker races to catch up at full Kafka fetch rate and **overwhelms the downstream Postgres sink** — connection pool exhausted, checkout API 503s.

The replay service still throttles with `MAX_REPLAY_EPS = 2000` from `@Value` at startup — too high for sink recovery, too low to tune without redeploy. The data SRE asks:

> "Replay rate is a **recovery posture knob** — why can't we drop `max_events_per_sec` to **400** live while the sink heals?"

Most Java replay workers encode throttle policy as **constants**, **Kafka consumer configs**, and **static application.yml** — none adjustable mid-catch-up. [Kiponos.io](https://kiponos.io) holds replay ceilings in profile `['replay']['prod']['limits']` with **local `getInt()` on every poll loop**.

## The problem: max_events_per_sec frozen during consumer recovery

```java
@Service
public class ReplayCatchUpWorker {
    private static final int MAX_EVENTS_PER_SEC = 2000;

    public void pollAndForward() {
        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
        int forwarded = 0;
        long windowStart = System.nanoTime();

        for (ConsumerRecord<String, byte[]> rec : records) {
            if (forwarded >= MAX_EVENTS_PER_SEC && elapsedSec(windowStart) < 1.0) {
                Thread.sleep(50);
                windowStart = System.nanoTime();
                forwarded = 0;
            }
            sinkWriter.write(rec);
            forwarded++;
        }
    }
}
```

Static config:

```yaml
replay:
  prod:
    limits:
      max_events_per_sec: 2000
```

During sink recovery you need to:

1. Lower **`max_events_per_sec`** to 400 immediately
2. Raise **`sink.backoff_on_pool_exhausted_ms`** dynamically
3. Enable **`gradual_ramp.enabled`** to step rate up as sink health returns

Redeploying replay workers mid-catch-up risks **duplicate processing** and **offset commit races**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Kafka fetch rate is the throttle" | Sink capacity — not broker — is the bottleneck |
| "We'll scale sink replicas" | Replay storm hits before autoscaler reacts |
| "Pause consumption in Kafka" | Ops wants controlled catch-up — not full stop |
| "Replay throttle is a one-time constant" | Optimal RPS changes hourly during recovery |
| "Flink handles replay" | Custom catch-up workers still exist beside Flink |

## The Aha

**`max_events_per_sec` is operational config** — it shifts during sink outages, pool exhaustion, and controlled catch-up. It belongs in profile `['replay']['prod']['limits']` with local `getInt()` every poll window.

## What Kiponos.io is for stream replay limits

[Kiponos.io](https://kiponos.io) hydrates `['replay']['prod']['limits']` into replay worker JVMs. Dashboard edits propagate via WebSocket delta; the next poll loop reads the new ceiling.

`afterValueChanged` logs throttle changes, notifies `#data-streaming`, and increments `replay_rate_change_total`.

No worker restart. No offset reset. Throttle applies on next second-boundary.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/zyrvmq.png)

## Config tree — limits, sink, gradual_ramp, topics, audit

Five folders — `limits`, `sink`, `gradual_ramp`, `topics`, `audit`:

```yaml
limits/
  max_events_per_sec: 2000
  min_events_per_sec: 100
  max_events_per_sec_ceiling: 10000
  enabled: true
sink/
  backoff_on_pool_exhausted_ms: 250
  pause_replay_on_sink_errors: true
  max_consecutive_sink_errors: 25
gradual_ramp/
  enabled: false
  start_events_per_sec: 400
  step_events_per_sec: 200
  step_interval_sec: 300
topics/
  inventory_deltas/
    max_events_per_sec: 2000
  order_events/
    max_events_per_sec: 5000
audit/
  last_change_by: ""
  siem_forward_enabled: true
```

Profile path: `['replay']['prod']['limits']`.

## Java integration: live replay throttle + afterValueChanged

```java
import io.kiponos.sdk.Kiponos;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LiveReplayCatchUpWorker {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final KafkaConsumer<String, byte[]> consumer;
    private final SinkWriter sinkWriter;

    public LiveReplayCatchUpWorker(KafkaConsumer<String, byte[]> consumer, SinkWriter sinkWriter) {
        this.consumer = consumer;
        this.sinkWriter = sinkWriter;
        kiponos.afterValueChanged(change -> {
            log.info("Replay rate delta: path={} value={}", change.path(), change.newValue());
            if (kiponos.path("audit").getBool("siem_forward_enabled")) {
                siemClient.emit("dataops_replay_rate_change", change.path(), change.newValue());
            }
        });
    }

    public void pollAndForward(String topic) {
        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
        int maxEps = resolveMaxEventsPerSec(topic);
        int forwarded = 0;
        long windowStart = System.nanoTime();

        for (ConsumerRecord<String, byte[]> rec : records) {
            if (sinkWriter.isPoolExhausted()) {
                var sink = kiponos.path("sink");
                if (sink.getBool("pause_replay_on_sink_errors")) {
                    sleepMs(sink.getInt("backoff_on_pool_exhausted_ms"));
                    continue;
                }
            }

            if (forwarded >= maxEps && elapsedSec(windowStart) < 1.0) {
                sleepMs(50);
                windowStart = System.nanoTime();
                forwarded = 0;
                maxEps = resolveMaxEventsPerSec(topic);
            }

            sinkWriter.write(rec);
            forwarded++;
        }
        consumer.commitSync();
    }

    private int resolveMaxEventsPerSec(String topic) {
        String folder = topic.replace("-", "_");
        var topicPath = kiponos.path("topics", folder);
        if (topicPath.exists()) {
            return applyGradualRamp(topicPath.getInt("max_events_per_sec"));
        }
        return applyGradualRamp(kiponos.path("limits").getInt("max_events_per_sec"));
    }

    private int applyGradualRamp(int base) {
        var ramp = kiponos.path("gradual_ramp");
        if (!ramp.getBool("enabled")) {
            return base;
        }
        int elapsed = rampState.elapsedSecSinceRecoveryStart();
        int steps = elapsed / ramp.getInt("step_interval_sec");
        int current = ramp.getInt("start_events_per_sec")
            + steps * ramp.getInt("step_events_per_sec");
        return Math.min(base, current);
    }
}
```

## Real-world scenarios

| Scenario | Without live replay tree | With Kiponos DataOps limits |
|----------|-------------------------|----------------------------|
| Sink pool exhausted | Full-speed replay; API 503 | `max_events_per_sec: 400` live |
| Gradual sink recovery | Manual restarts with new args | `gradual_ramp/enabled: true` |
| Per-topic catch-up | One global throttle | `topics/inventory_deltas` isolated |
| Catch-up complete | Redeploy to restore 2000 | Dashboard reset |
| Postmortem throttle audit | Git constants | Kiponos ACL + SIEM |

## Performance: replay throttle on poll loop

- **One WebSocket per replay JVM** — not HTTP per Kafka poll
- **Rate resolve is 2 local reads** — nanoseconds vs sink write RTT
- **Delta patches** — throttle drops in seconds without offset reset
- **Gradual ramp reads same tree** — coordinated recovery posture
- **Sink backoff keys** colocated with rate limits

## Compare to alternatives

| Approach | Mid-recovery throttle drop | Worker restart | Per-topic + gradual ramp |
|----------|---------------------------|----------------|--------------------------|
| application.yml + redeploy | No — offset risk | Required | Partial |
| Kafka pause/resume | Binary — no partial rate | No | No |
| Manual sleep in code | Requires deploy | Yes | No |
| Redis rate limiter | Poll latency | Possible | Custom |
| **Kiponos SDK** | **Seconds** | **None** | **Yes** |

## When not to use Kiponos for replay limits

| Boundary | Better home |
|----------|-------------|
| Kafka topic retention and compaction | Broker admin |
| Consumer group offset reset policy | Runbook / CLI tooling |
| Sink connection pool max size | HikariCP / infra config |
| Exactly-once replay semantics | Application design |
| Postgres primary failover | DBA / Patroni |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['replay']['prod']['limits']`.
2. Add `io.kiponos:sdk-boot-3` to replay worker service.
3. Set `-Dkiponos="['replay']['prod']['limits']"`.
4. Replace `MAX_EVENTS_PER_SEC` with `resolveMaxEventsPerSec(topic)`.
5. Wire `afterValueChanged` SIEM forwarding.
6. Drill: staging — simulate sink exhaustion and lower `max_events_per_sec` — confirm write rate drops **without worker restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Kafka consumer lag thresholds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-kafka-consumer-lag-thresholds.md)
- Related: [Retry backoff live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-retry-backoff-live.md)

---

*Kiponos.io — Kafka offsets live in the broker; max_events_per_sec lives in the tree.*