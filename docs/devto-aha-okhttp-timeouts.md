---
main_image: https://litter.catbox.moe/8zkxr8.jpg
title: "OkHttp connectTimeout(10, SECONDS) Was Client Folklore — We Changed Timeouts Live Mid-Outage (Java)"
published: false
tags: java, httpclient, resilience, devops
description: OkHttp connect/read/write timeouts are usually final at client build. When a dependency degrades, Kiponos feeds live timeout and retry policy without rebuilding the OkHttpClient in a release.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-okhttp-timeouts.md
---

Thursday 03:19. Payments calls a card network over **OkHttp**. Their edge is slow — not dead. Your client was built once:

```java
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build();
```

P99 climbs past 30s. Threads pile on `readTimeout`. Someone wants **8s** fail-fast to trip the circuit breaker earlier. That number is trapped in a `final` client from startup.

> "OkHttp timeouts are **client construction**. New values need a release."

**The Aha:** connect/read/write timeouts and call timeouts are **dependency health policy** — operational, not folklore. [Kiponos.io](https://kiponos.io) holds them live; you either rebuild a lightweight client on change or set per-call timeouts from local gets.

## Belief vs production

| Belief | Reality |
|--------|---------|
| 30s read is "safe" | Safe for you, lethal for thread pools |
| One client forever | Partner SLOs change by region and hour |
| Retry in code constants | Retries without live budgets amplify outages |

## Kiponos shape

```yaml
http_ops/
  card_network/
    connect_timeout_ms: 3000
    read_timeout_ms: 8000
    write_timeout_ms: 8000
    call_timeout_ms: 10000
    max_retries: 1
    retry_backoff_ms: 200
```

Profile: `['payments']['v3']['prod']['base']`. Hot path: local `getInt` — zero network.

## Architecture

![Architecture diagram](https://files.catbox.moe/qrkb08.png)

## Integration

```java
@Component
public class LiveCardNetworkHttp {

    private final Kiponos kiponos;
    private final AtomicReference<OkHttpClient> client = new AtomicReference<>();

    public LiveCardNetworkHttp(Kiponos kiponos) {
        this.kiponos = kiponos;
        client.set(build());
        kiponos.afterValueChanged(ch -> {
            if (ch.path().startsWith("http_ops/card_network")) {
                client.set(build());
            }
        });
    }

    private OkHttpClient build() {
        var h = kiponos.path("http_ops", "card_network");
        return new OkHttpClient.Builder()
            .connectTimeout(h.getLong("connect_timeout_ms", 3000), TimeUnit.MILLISECONDS)
            .readTimeout(h.getLong("read_timeout_ms", 8000), TimeUnit.MILLISECONDS)
            .writeTimeout(h.getLong("write_timeout_ms", 8000), TimeUnit.MILLISECONDS)
            .callTimeout(h.getLong("call_timeout_ms", 10000), TimeUnit.MILLISECONDS)
            .build();
    }

    public String post(String url, RequestBody body) throws IOException {
        var h = kiponos.path("http_ops", "card_network");
        int attempts = 1 + h.getInt("max_retries", 1);
        IOException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                Request req = new Request.Builder().url(url).post(body).build();
                try (Response resp = client.get().newCall(req).execute()) {
                    return resp.body() != null ? resp.body().string() : "";
                }
            } catch (IOException e) {
                last = e;
                sleep(h.getLong("retry_backoff_ms", 200));
            }
        }
        throw last;
    }
}
```

Per-call override without full rebuild:

```java
int readMs = kiponos.path("http_ops", "card_network").getInt("read_timeout_ms", 8000);
Request req = new Request.Builder().url(url).get().build();
try (Response resp = client.get().newBuilder()
        .readTimeout(readMs, TimeUnit.MILLISECONDS)
        .build()
        .newCall(req)
        .execute()) {
    // ...
}
```

## Scenarios

| Moment | Frozen reflex | Live |
|--------|---------------|------|
| Slow partner | Hope / deploy | Drop `read_timeout_ms` to 5s |
| Regional blip | Global 60s | Profile-specific timeouts |
| Load test | Hard-coded | Hub `loadtest` tree |

Pair with [Feign timeouts](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-feign-read-timeout.md) and [WebClient](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-http-timeout.md).

## Before / after

| Approach | Change timeout mid-outage | Hot path |
|----------|---------------------------|----------|
| `final` client | Redeploy | Frozen |
| Config server poll per call | Possible | Network RTT |
| **Kiponos** | **Delta + rebuild or per-call** | **Local get** |

## When not

| Case | Prefer |
|------|--------|
| mTLS cert rotation | Secret manager + restart policy |
| Base URL environment | Deploy-time wiring |
| Protocol change HTTP→gRPC | Architecture |

## Performance notes

- Rebuild OkHttpClient on delta is cheap compared to an outage; prefer that over polling a config server on every call.  
- `getInt` / `getLong` remain O(1) local memory — safe inside retry loops.  
- Do not put secrets, base URLs, or mTLS material in the live tree; only **operational floats**.  
- Dispatcher thread pool sizes are a separate dial — pair with server-side thread articles when both sides thrash.

## Getting started

1. Move four timeout ints + retry budget into hub  
2. AtomicReference client rebuild on `afterValueChanged`  
3. Game day: inject latency, tighten read timeout from dashboard  
4. Keep base URL and certs out of the live tree  
5. Document which profile owns card-network vs warehouse HTTP so on-call never edits the wrong tree  

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) · [GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)

---

*Kiponos.io — HTTP client timeouts are dependency health policy, not folklore.*

