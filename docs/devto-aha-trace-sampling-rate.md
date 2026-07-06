---
title: "traceidratio=0.1 Was Observability Budget Law — We Cranked It Live During the Outage Hunt (OpenTelemetry Java)"
published: false
tags: java, opentelemetry, observability, devops
description: OpenTelemetry trace sampling ratio feels like observability budget frozen in agent config. When incidents demand full visibility, sampling rate is operational — Kiponos feeds live tracing policy without JVM restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-trace-sampling-rate.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-aha-trace-sampling-rate.jpg
---

Checkout outage minute 23. Support tickets flood in but Jaeger shows **three spans** for the failing path — because `otel.traces.sampler.arg=0.1` has been production gospel since FinOps capped observability spend at 10% sampling. The on-call SRE cannot reconstruct the failure chain.

Observability lead says what every platform team has memorized:

> "Sampling ratio is **budget policy**. We do not raise it without FinOps approval and collector capacity review."

But the outage bridge does not wait for FinOps. Sampling rate is not accounting scripture — it is **what fraction of reality you record tonight** while debugging a revenue path.

**The Aha:** read `trace_sampling_ratio` from [Kiponos.io](https://kiponos.io) in your `Sampler` implementation — ops sets `1.0` live while checkout pods keep serving traffic.

## The problem: sampling ratio frozen at SDK initialization

```yaml
otel:
  traces:
    sampler: traceidratio
    sampler.arg: 0.1
```

```java
@Configuration
public class OtelConfig {

    private static final double TRACE_SAMPLING_RATIO = 0.1;

    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .setSampler(Sampler.traceIdRatioBased(TRACE_SAMPLING_RATIO))
                                .build())
                .build();
    }
}
```

Or ratio set once in `OTEL_TRACES_SAMPLER_ARG` env at pod start. Problems during incidents:

1. **Blind debugging** — 90% of failing requests leave no trace
2. **Deploy to raise** — while outage continues
3. **Stay at 100% forever** — collector bill explodes unless someone reverts

| What teams say | What production does |
|----------------|---------------------|
| "10% was FinOps-approved observability budget" | Incidents need temporary 100% on critical paths |
| "Tail sampling in collector is enough" | Tail sampling still needs **head** samples to arrive |
| "Logs are the outage source of truth" | Logs without trace correlation miss cross-service causality |
| "OTel sampler args belong in Helm values" | Sampling ratio is operational visibility dial |

## What is Kiponos.io — for OpenTelemetry sampling policy

[Kiponos.io](https://kiponos.io) stores operational tracing knobs under profile `['checkout']['prod']['otel']`. WebSocket deltas patch the in-memory tree. `getDouble("trace_sampling_ratio")` is a **local read** inside your custom `Sampler` — no config server RTT per span decision.

Git keeps **exporter endpoint and service name**; the hub keeps **sampling ratio this outage**.

## Architecture

![Architecture diagram](https://files.catbox.moe/rnhazd.png)

## Config tree

```yaml
otel/
  tracing/
    checkout/
      trace_sampling_ratio: 0.1
      enabled: true
      always_sample_errors: true
    payments/
      trace_sampling_ratio: 0.25
      enabled: true
  ops/
    outage_debug_mode: false
    outage_sampling_ratio: 1.0
    max_outage_duration_min: 60
  collector/
    warn_high_cardinality: true
    drop_healthcheck_spans: true
```

## Integration (Spring Boot OpenTelemetry)

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

```java
@Component
public class LiveRatioSampler implements Sampler {

    private final Kiponos kiponos;
    private final Sampler parent;

    public LiveRatioSampler(Kiponos kiponos) {
        this.kiponos = kiponos;
        this.parent = Sampler.parentBased(this);
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("otel/tracing")
                    || change.path().startsWith("otel/ops")) {
                log.warn("OTel sampling policy: {} → {}", change.path(), change.newValue());
            }
        });
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        var cfg = kiponos.path("otel", "tracing", "checkout");
        if (!cfg.getBool("enabled", true)) {
            return SamplingResult.drop();
        }
        if (cfg.getBool("always_sample_errors", true)
                && attributes.get(AttributeKey.stringKey("error")) != null) {
            return SamplingResult.recordAndSample();
        }
        double ratio = resolveSamplingRatio();
        return Sampler.traceIdRatioBased(ratio).shouldSample(
                parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    private double resolveSamplingRatio() {
        if (kiponos.path("otel", "ops").getBool("outage_debug_mode", false)) {
            return kiponos.path("otel", "ops").getDouble("outage_sampling_ratio", 1.0);
        }
        return kiponos.path("otel", "tracing", "checkout").getDouble("trace_sampling_ratio", 0.1);
    }

    @Override
    public String getDescription() {
        return "LiveRatioSampler(ratio=" + resolveSamplingRatio() + ")";
    }
}
```

Wire `LiveRatioSampler` into `SdkTracerProvider` at startup. Ratio value updates **live** via Kiponos — no tracer provider rebuild per request.

Outage hunt? Ops enables `outage_debug_mode` and `outage_sampling_ratio: 1.0`. Next checkout requests produce full traces — **without pod restart**.

## Real scenarios

| Event | `TRACE_SAMPLING_RATIO = 0.1` budget law | Kiponos path |
|-------|----------------------------------------|--------------|
| Checkout outage | Jaeger nearly empty on failure path | `outage_debug_mode: true` live |
| Root cause found | Still 100% sampling burning collector | Disable outage mode from dashboard |
| Black Friday prep | Permanent YAML branch debate | Hub profile `peak/debug_checkout` |
| FinOps audit | Helm values from Q1 | Dashboard audit on `otel/ops` |

## Performance — why tracing overhead stays controlled

- **`getDouble()` once per `shouldSample` call** — O(1) vs span export I/O
- **One WebSocket** per checkout JVM
- **Return to 10% after outage** — one dashboard toggle protects collector bill
- **Delta updates** — outage mode toggles two keys instantly
- **`always_sample_errors` optional** — error paths visible even at low ratio

## Compare to alternatives

| Approach | Raise sampling during outage | Per-span decision cost |
|----------|---------------------------|------------------------|
| `OTEL_TRACES_SAMPLER_ARG=0.1` env | Rolling restart | Zero (frozen) |
| Collector tail sampling only | No head samples to tail | N/A |
| Poll Redis per span | Possible | RTT × millions of spans |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for OTel sampling

| Case | Better approach |
|------|-----------------|
| Collector cluster sizing and retention | Infrastructure GitOps |
| Exporter endpoint and TLS | Git + Helm |
| Switching Jaeger → Tempo backend | Migration project |
| `outage_sampling_ratio: 1.0` forever on 500-pod fleet | FinOps capacity plan first |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['checkout']['prod']['otel']`.
2. Add `io.kiponos:sdk-boot-3` to your instrumented service.
3. Create `otel/tracing/checkout` with ratio and outage keys.
4. Replace `traceIdRatioBased(0.1)` constant with `LiveRatioSampler`.
5. Staging: inject checkout failure, enable `outage_debug_mode`, confirm Jaeger fills **without pod restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — trace sampling ratio is what you choose to see tonight, not FinOps law etched in Helm.*