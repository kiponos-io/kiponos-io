---
title: "feign.client.config.default.readTimeout=5000 Was Platform Policy — We Extended It Live During the Partner Brownout (OpenFeign)"
published: false
tags: java, springcloud, openfeign, resilience
description: OpenFeign readTimeout feels like platform policy frozen in application.yml. When partner APIs slow down, read timeout milliseconds are operational — Kiponos feeds live Feign policy without service restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-feign-read-timeout.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-aha-feign-read-timeout.jpg
---

Tax filing integration minute 31. The government partner API still returns HTTP 200 — eventually — but **p99 crosses 18 seconds** while your orchestration service fails at 5 seconds because `feign.client.config.default.readTimeout=5000` ships in the org-wide Spring Cloud starter every team inherits without reading.

Half the batch jobs show `ReadTimeoutException`. Compliance dashboards go yellow. The integration manager says:

> "Five seconds is our **platform standard** for outbound HTTP. We do not loosen Feign timeouts without architecture board approval."

But the architecture board meets on Tuesdays. Read timeout is not philosophy — it is **how long tonight's batch waits for tonight's partner latency**.

**The Aha:** read `read_timeout_ms` from [Kiponos.io](https://kiponos.io) when building each Feign `Request.Options` — ops sets `20000` live while workers keep running.

## The problem: Feign timeout frozen at bean creation

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 2000
        readTimeout: 5000
      tax-partner:
        readTimeout: 5000
```

```java
@FeignClient(name = "tax-partner", url = "${tax.partner.url}")
public interface TaxPartnerClient {
    @PostMapping("/v2/filings/submit")
    FilingReceipt submit(@RequestBody FilingPayload payload);
}
```

Spring Cloud OpenFeign reads timeouts when the client bean materializes. Changing YAML means **pod restart** while filings fail every 5 seconds. `@RefreshScope` on Feign clients recreates beans mid-batch — risky.

Your batch worker calls the client in a tight loop:

```java
@Service
public class FilingBatchService {

    private final TaxPartnerClient client;

    public void processQueue(List<FilingPayload> batch) {
        for (FilingPayload payload : batch) {
            client.submit(payload); // fails at 5s while partner needs 12s
        }
    }
}
```

| What teams say | What production does |
|----------------|---------------------|
| "Fail fast at 5s protects thread pools" | False failures flood retries and worsen partner load |
| "Partner will fix latency soon" | Compliance windows do not wait |
| "We'll override per-client in YAML" | Still needs deploy per incident |
| "Feign config is platform infrastructure" | Read timeout is operational patience per partner |

## What is Kiponos.io — for OpenFeign timeouts

[Kiponos.io](https://kiponos.io) stores operational HTTP client knobs under profile `['tax']['prod']['feign']`. WebSocket deltas patch the in-memory tree. `getInt("read_timeout_ms")` is a **local read** when constructing request options — no Redis RTT on every filing call.

Git keeps **partner base URL and OAuth wiring**; the hub keeps **read milliseconds this filing season**.

## Architecture

![Architecture diagram](https://litter.catbox.moe/97n6a1.png)

## Config tree

```yaml
feign/
  clients/
    tax_partner/
      read_timeout_ms: 5000
      connect_timeout_ms: 2000
      enabled: true
      brownout_mode: false
      brownout_read_timeout_ms: 20000
    identity_verify/
      read_timeout_ms: 3000
      connect_timeout_ms: 1000
  ops/
    partner_storm: false
    storm_read_timeout_ms: 25000
  retry/
    max_attempts: 2
    backoff_ms: 500
```

## Integration (Spring Boot OpenFeign)

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
public class LiveFeignOptionsInterceptor implements RequestInterceptor {

    private final Kiponos kiponos;

    public LiveFeignOptionsInterceptor(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("feign/clients/tax_partner")) {
                log.warn("Feign timeout policy: {} → {}", change.path(), change.newValue());
            }
        });
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!"tax-partner".equals(template.feignTarget().name())) return;
        var cfg = kiponos.path("feign", "clients", "tax_partner");
        if (!cfg.getBool("enabled", true)) {
            throw new ServiceUnavailableException("tax-partner client disabled");
        }
        int readMs = resolveReadTimeoutMs(cfg);
        int connectMs = cfg.getInt("connect_timeout_ms", 2000);
        template.requestOptions(new Request.Options(connectMs, TimeUnit.MILLISECONDS,
                readMs, TimeUnit.MILLISECONDS, true));
    }

    private int resolveReadTimeoutMs(ConfigPath cfg) {
        if (kiponos.path("feign", "ops").getBool("partner_storm", false)) {
            return kiponos.path("feign", "ops").getInt("storm_read_timeout_ms", 25000);
        }
        if (cfg.getBool("brownout_mode", false)) {
            return cfg.getInt("brownout_read_timeout_ms", 20000);
        }
        return cfg.getInt("read_timeout_ms", 5000);
    }
}
```

Partner brownout? Ops enables `partner_storm` and `storm_read_timeout_ms: 25000`. Next Feign calls wait longer — **no batch worker restart**.

## Real scenarios

| Event | `readTimeout: 5000` platform policy | Kiponos path |
|-------|-----------------------------------|--------------|
| Partner latency spike | Mass `ReadTimeout`, filings stuck | `partner_storm: true` live |
| Partner recovered | Still waiting 25s until deploy | Disable storm mode from dashboard |
| Filing deadline week | Emergency YAML branch | Hub profile `filing-season/patient` |
| Audit trail | Git blame on starter POM | Dashboard history on `feign/ops` |

Pair with [live HTTP timeout patterns](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-http-timeout.md) for non-Feign clients in the same integration mesh.

## Performance — why batch throughput stays healthy

- **`getInt()` once per Feign invocation** — not per retry attempt inside okhttp
- **One WebSocket** per orchestration JVM
- **Longer timeout reduces false failures** — fewer retry storms against partner
- **Delta merge async** — storm toggle patches two keys instantly
- **Interceptor runs on calling thread** — but read is O(1) memory

## Compare to alternatives

| Approach | Extend readTimeout during brownout | Per-call overhead |
|----------|-----------------------------------|-------------------|
| `application.yml` Feign config | Rolling restart | Zero (frozen) |
| `@RefreshScope` Feign beans | Context recycle | Bean churn mid-batch |
| Poll Redis per request | Possible | RTT × thousands of filings |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for Feign timeouts

| Case | Better approach |
|------|-----------------|
| Partner URL migration | Git-reviewed config |
| OAuth client credentials rotation | Secrets manager |
| Replacing Feign with WebClient reactive | Code migration |
| readTimeout of 120s on internal sub-ms service | Fix routing |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['tax']['prod']['feign']`.
2. Add `io.kiponos:sdk-boot-3` and register `LiveFeignOptionsInterceptor`.
3. Create `feign/clients/tax_partner` tree with storm keys.
4. Remove hard-coded `readTimeout` from inherited starter YAML for this client.
5. Staging: inject partner latency, enable `partner_storm`, watch batch success recover **without worker restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — Feign readTimeout is tonight's patience, not platform policy carved in the starter.*