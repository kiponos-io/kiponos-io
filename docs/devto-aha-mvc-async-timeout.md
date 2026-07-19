---
main_image: 
title: "spring.mvc.async.request-timeout Looked Harmless — We Changed Async Request Budgets Live When Tomcat Threads Vanished"
published: false
tags: java, springboot, web, devops
description: Spring MVC async request timeouts are often fixed at startup. When slow downstreams hold async requests open, Kiponos lets you tighten async budgets live without a redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-mvc-async-timeout.md
---

Saturday 19:18. You "went async" so Tomcat would not block. Now `DeferredResult` / `Callable` handlers sit open for **60s** each because:

```properties
spring.mvc.async.request-timeout=60000
```

Downstream is crawling. Async did not save you — it **hid** the thread pool pain behind a longer bill.

> "Async timeout is web framework config. Change on deploy."

**The Aha:** async request timeout is a **user-visible budget**. [Kiponos.io](https://kiponos.io) holds per-endpoint budgets live so on-call can fail faster (or grant a known long route more time) without a release.

## Belief vs production

| Belief | Reality |
|--------|---------|
| Async = infinite patience | Users leave; clients retry; storms worsen |
| One global 60s | Upload vs status-poll need different budgets |
| Only Tomcat threads matter | Async still ties memory, connections, DB |

## Kiponos shape

```yaml
web_ops/
  async/
    default_timeout_ms: 10000
    endpoints/
      checkout_quote:
        timeout_ms: 5000
      export_job:
        timeout_ms: 120000
```

## Integration sketch

```java
long ms = kiponos.path("web_ops", "async", "endpoints", "checkout_quote")
    .getLong("timeout_ms", 5000);
DeferredResult<Quote> dr = new DeferredResult<>(ms);
// or WebAsyncTask with live timeout supplier refreshed on delta
```

Pair with [Tomcat threads](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tomcat-threads.md) and [gateway timeouts](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-gateway-response-timeout.md).

## Scenarios

| Moment | Frozen | Live |
|--------|--------|------|
| Downstream brownout | 60s zombie requests | Drop default to 8s |
| Known bulk export | Kills job | Open export endpoint only |
| Mobile clients | Angry retries | Align with client timeouts |

## Before / after

| Approach | Mid-incident | Hot path |
|----------|--------------|----------|
| properties + restart | Slow | Frozen |
| **Kiponos** | **Seconds** | **Local get** |

## When not

| Case | Prefer |
|------|--------|
| Move to pure reactive stack | Architecture project |
| Auth session redesign | Code |
| File upload size limits | Separate dial |

## Getting started

1. Split default vs long-running endpoint budgets  
2. Construct `DeferredResult` / `WebAsyncTask` with live ms  
3. Emit metric when async timeout fires by endpoint  
4. Keep aligned with edge gateway response timeout  

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) · [GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)

---

*Kiponos.io — async timeouts are user budgets, not a free lunch.*
