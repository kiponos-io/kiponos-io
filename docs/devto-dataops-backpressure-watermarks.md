---
title: "Backpressure Watermarks Live — Flink/Stream Recovery Knobs (Java SDK)"
published: false
tags: java, flink, data, streaming
description: Watermark delays in job config need restarts. Kiponos holds watermark ms for streaming ops — complements Flink checkpoint article.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-backpressure-watermarks.md
main_image: https://files.catbox.moe/e166i8.jpg
---

Wednesday 22:08 UTC. **clickstream-events** Flink job shows **late-event surge** — mobile clients batch-upload timestamps up to twelve seconds behind wall clock. Windows close early; attribution counts drop 8%. Watermark lag is still `MAX_OUT_OF_ORDERNESS = 5000` ms from job submit args six months ago.

The streaming on-call wants `max_out_of_orderness_ms` widened to **15000** for the next two hours — not cancel-and-resubmit from savepoint. The data platform lead asks:

> "Watermark tolerance is a **runtime recovery knob** — why does widening it require **job cancellation** when only one long changed?"

Most Flink deployments freeze watermark policy in **CLI args**, **flink-conf.yaml**, and **hard-coded `Duration.ofMillis(5000)`** in custom assigners. [Kiponos.io](https://kiponos.io) holds watermark parameters in profile `['flink']['prod']['watermarks']` — readable by embedded policy operators with **local `get*()`**.

## The problem: max_out_of_orderness_ms frozen at job submit

```java
public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.getConfig().setAutoWatermarkInterval(200L);

    DataStream<ClickEvent> clicks = env
        .addSource(kafkaSource)
        .assignTimestampsAndWatermarks(
            WatermarkStrategy
                .<ClickEvent>forBoundedOutOfOrderness(Duration.ofMillis(5000))
                .withTimestampAssigner((e, ts) -> e.getEventTimeMs())
        );
    // ...
    env.execute("clickstream-events");
}
```

Cluster-wide static config:

```yaml
# flink-conf.yaml — not per-job live
pipeline.auto-watermark-interval: 200
```

During late-event surge you need to:

1. Raise **`watermarks.max_out_of_orderness_ms`** to 15000
2. Increase **`watermarks.idle_source_timeout_ms`** for sparse partitions
3. Toggle **`backpressure.widen_on_lag`** when consumer lag exceeds guard

Cancel-and-resubmit while attribution dashboards go red is **streaming ops debt**.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Watermark delay is a design-time constant" | Mobile batching patterns shift seasonally |
| "Flink auto watermark interval is enough" | Out-of-orderness bound is separate and critical |
| "Savepoint restart is fast" | Minutes of mis-attribution at peak traffic |
| "One watermark policy per cluster" | Clickstream and billing need different bounds |
| "We'll tune in platform sprint" | Campaign launch ends before PR merges |

## The Aha

**`max_out_of_orderness_ms` is operational config** — it shifts during late-event surges, source outages, and backpressure incidents. It belongs in profile `['flink']['prod']['watermarks']` with live reads in a policy operator.

## What Kiponos.io is for Flink watermarks

[Kiponos.io](https://kiponos.io) embeds in a `ProcessFunction` or dedicated watermark policy operator. Profile `['flink']['prod']['watermarks']` hydrates at `open()`; deltas apply between watermark emissions.

`afterValueChanged` logs policy shifts and increments `watermark_policy_change_total` — **without** canceling the job.

Complements [Flink checkpoint interval live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-flink-checkpoint-interval.md) — checkpoints tune recovery; watermarks tune event-time correctness under load.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/6apwrb.png)

## Config tree — watermarks, backpressure, sources, lag, audit

Five folders — `watermarks`, `backpressure`, `sources`, `lag`, `audit`:

```yaml
watermarks/
  max_out_of_orderness_ms: 5000
  auto_watermark_interval_ms: 200
  idle_source_timeout_ms: 30000
  allowed_lateness_ms: 1000
backpressure/
  widen_on_lag: true
  lag_threshold_events: 100000
  widened_out_of_orderness_ms: 15000
sources/
  clickstream/
    max_out_of_orderness_ms: 5000
  billing/
    max_out_of_orderness_ms: 2000
lag/
  consumer_group: clickstream-prod
  alert_threshold: 50000
audit/
  last_change_by: ""
  siem_forward_enabled: true
```

Profile path: `['flink']['prod']['watermarks']`.

## Java integration: live watermark policy operator

```java
import io.kiponos.sdk.Kiponos;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

public class LiveWatermarkPolicyOperator extends ProcessFunction<ClickEvent, ClickEvent> {
    private transient Kiponos kiponos;
    private transient long lastAppliedOooMs;

    @Override
    public void open(Configuration parameters) {
        kiponos = Kiponos.createForCurrentTeam();
        lastAppliedOooMs = kiponos.path("watermarks").getLong("max_out_of_orderness_ms");

        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("watermarks/") || change.path().startsWith("backpressure/")) {
                getRuntimeContext().getMetricGroup().counter("watermark_policy_change").inc();
                log.info("Watermark policy delta: path={} value={}", change.path(), change.newValue());
            }
        });
    }

    @Override
    public void processElement(ClickEvent event, Context ctx, Collector<ClickEvent> out) {
        maybeUpdateWatermarkPolicy();
        out.collect(event);
    }

    private void maybeUpdateWatermarkPolicy() {
        long targetOoo = resolveOutOfOrdernessMs();

        if (targetOoo != lastAppliedOooMs) {
            watermarkCoordinator().updateMaxOutOfOrderness(targetOoo);
            watermarkCoordinator().updateAutoWatermarkInterval(
                kiponos.path("watermarks").getLong("auto_watermark_interval_ms"));
            lastAppliedOooMs = targetOoo;
        }
    }

    private long resolveOutOfOrdernessMs() {
        var bp = kiponos.path("backpressure");
        if (bp.getBool("widen_on_lag")) {
            long lag = lagClient().currentLag(kiponos.path("lag").get("consumer_group"));
            if (lag > bp.getLong("lag_threshold_events")) {
                return bp.getLong("widened_out_of_orderness_ms");
            }
        }
        return kiponos.path("sources", "clickstream").getLong("max_out_of_orderness_ms",
            kiponos.path("watermarks").getLong("max_out_of_orderness_ms"));
    }
}
```

`watermarkCoordinator().updateMaxOutOfOrderness()` is your thin adapter — Kiponos feeds live longs; Flink applies between watermark ticks.

## Real-world scenarios

| Scenario | Without live watermark tree | With Kiponos DataOps watermarks |
|----------|------------------------------|--------------------------------|
| Mobile late-event surge | Cancel job; savepoint; resubmit | `max_out_of_orderness_ms: 15000` live |
| Kafka lag spike | Manual intervention | `backpressure/widen_on_lag` auto-widens |
| Campaign ends | Second resubmit | Reset watermarks in dashboard |
| Billing job stricter bound | Shared flink-conf | `sources/billing` isolated key |
| Postmortem policy audit | CLI args in git | Kiponos ACL + metrics |

## Performance: watermark policy on event path

- **One WebSocket per job operator** — not HTTP per event
- **Policy check is ≤5 local long reads** — nanoseconds vs Kafka fetch
- **Delta patches** — one ms value without TaskManager config reload
- **Apply between watermark ticks** — no mid-window corruption
- **Source-specific overrides** in one tree — clickstream vs billing

## Compare to alternatives

| Approach | Mid-surge widen | Hot-path latency | Per-source + backpressure guard |
|----------|----------------|------------------|--------------------------------|
| CLI args at submit | No — cancel/resubmit | Static | No |
| flink-conf.yaml | TM restart | Cluster-wide | Partial |
| Flink REST dynamic props | Limited | HTTP per update | Vendor-dependent |
| **Kiponos SDK** | **Dashboard delta** | **Zero (in-process)** | **Yes** |

## When not to use Kiponos for watermarks

| Boundary | Better home |
|----------|-------------|
| State backend, checkpoint storage | Job submit / GitOps |
| Parallelism and rescaling | Flink autoscaler |
| Kafka ACLs and broker certs | Infra / Vault |
| Event-time semantics design | Architecture docs |
| Savepoint retention | Object storage policy |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['flink']['prod']['watermarks']`.
2. Add `io.kiponos:sdk-java` to Flink job JAR; embed `LiveWatermarkPolicyOperator`.
3. Set `-Dkiponos="['flink']['prod']['watermarks']"` on JobManager.
4. Implement `watermarkCoordinator()` adapter for your Flink version.
5. Wire `afterValueChanged` metrics.
6. Drill: staging — inject late events and widen `max_out_of_orderness_ms` — confirm window completeness improves **without cancel-and-resubmit**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Flink checkpoint interval live](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-flink-checkpoint-interval.md)
- Related: [Kafka consumer lag thresholds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-kafka-consumer-lag-thresholds.md)

---

*Kiponos.io — submit args boot the job; max_out_of_orderness_ms lives in the tree.*