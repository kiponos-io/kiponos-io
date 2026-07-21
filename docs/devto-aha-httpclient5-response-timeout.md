---
title: "HttpClient 5 responseTimeout Felt Permanent — We Raised It Live While the Partner API Limped"
published: false
tags: java, httpclient, resilience, devops
description: Apache HttpClient 5 response timeouts are usually baked into a CloseableHttpClient bean at startup. When a partner API limps at noon, that integer should be ops-owned — Kiponos feeds live responseTimeout without rebuilding the client factory.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-httpclient5-response-timeout.md
main_image: https://files.catbox.moe/xvnh3m.jpg
---

# HttpClient 5 `responseTimeout` Felt Permanent — We Raised It Live While the Partner API Limped

Partner API latency graph looks like a heart monitor on a bad day. P95 jumps from 180ms to 2.4s. Your service is fine. Their on-call is “investigating.” Your thread pool is not fine — every outbound call still uses `Timeout.ofSeconds(1)` because someone set it in a `@Bean` in 2023 and called it “safe defaults.”

The PR to change one second to three is green. Deploy window is Thursday. It is Tuesday noon. Money is waiting.

**The Aha:** `responseTimeout` is not a compile-time moral stance. It is an **operational dial**. Read it from [Kiponos.io](https://kiponos.io) when you build the request config — or refresh the request config on each call from SDK memory — so ops can move the dial while the JVM keeps serving.

## The problem: timeout frozen in the client factory

```java
@Configuration
public class PartnerHttpConfig {

    @Bean(destroyMethod = "close")
    public CloseableHttpClient partnerClient() {
        return HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setResponseTimeout(Timeout.ofSeconds(1)) // "safe"
                                .build())
                .build();
    }
}
```

That bean is honest. It is also a hostage situation. Changing the integer means:

1. Edit code  
2. Review  
3. Build  
4. Deploy  
5. Hope the partner recovers on your schedule  

When the partner is limping *now*, “safe defaults” become **unsafe product behavior**: cascades, thread starvation, false 503s to your own clients.

## Architecture: dial on the wire, not in the jar

| Layer | Responsibility |
|-------|----------------|
| **Partner HTTPS** | External, slow, not under your deploy train |
| **HttpClient 5** | Executes requests with a **response timeout** |
| **SDK in-process cache** | Holds `partner.http.response-timeout-ms` with **no RTT** on hot path |
| **Kiponos hub + dashboard** | Ops raises 1000 → 3000 while the incident is live |
| **WebSocket deltas** | Patch the cache before the next outbound call |

```text
┌────────────┐   WS delta    ┌──────────────┐
│ Kiponos.io │ ───────────► │ SDK cache    │
│ dashboard  │              │ timeout-ms   │
└────────────┘              └──────┬───────┘
                                   │ getInt (local)
                                   ▼
                            RequestConfig
                                   │
                                   ▼
                            Partner API
```

## Code: request config from live memory

```java
public final class PartnerCaller {
    private final CloseableHttpClient client;
    private final Kiponos kiponos; // connected hub client

    public String getOrder(String id) throws IOException {
        int timeoutMs = kiponos.getInt(
                "partner/http/response-timeout-ms",
                1000 // safe default if hub never seen
        );

        RequestConfig cfg = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(timeoutMs))
                .build();

        HttpGet get = new HttpGet("https://partner.example/orders/" + id);
        get.setConfig(cfg);

        try (CloseableHttpResponse resp = client.execute(get)) {
            return EntityUtils.toString(resp.getEntity());
        }
    }
}
```

Notes that matter in production:

- **Default 1000ms** remains fail-closed if the hub was never reached.  
- Ops can set `3000` during limp mode without a deploy.  
- When the partner recovers, drop back to `1000` live — no “leave the elevated timeout forever” regret.

## Incident playbook (one screen)

| Minute | Action |
|--------|--------|
| 0 | Alert: partner p95 high, your pool queueing |
| 1 | Confirm not *your* bug (logs, dependency map) |
| 2 | Dashboard: `partner/http/response-timeout-ms` → `3000` |
| 3 | Watch error rate and pool usage — should bend |
| N | Partner healthy again → timeout → `1000` live |

No war-room debate about “is a mid-day deploy worth it for an integer.”

## What this is not

| Not this | Why |
|----------|-----|
| Infinite timeout | You still want a ceiling |
| Feature-flag “enable partner” only | You need a **number**, not a boolean |
| Restart-the-pod to reload YAML | That is the disease this dial cures |

## Try the pattern

1. Put timeout under a dedicated folder in Kiponos (not a flat random key).  
2. Default conservatively in code.  
3. Raise live under partner degradation.  
4. Return to baseline when the graph does.

People should not have to ship a release to make a decision about how long they are willing to wait on a limping friend.

---

*Kiponos — live nested config with WebSocket deltas and an in-process cache so hot paths stay local.*  
[kiponos.io](https://kiponos.io) · [GitHub examples](https://github.com/kiponos-io/kiponos-io)
