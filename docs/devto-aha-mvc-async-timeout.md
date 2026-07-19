---
main_image: https://files.catbox.moe/l631hl.jpg
title: "spring.mvc.async.request-timeout Looked Harmless — We Changed Async Request Budgets Live When Tomcat Threads Vanished"
published: false
tags: java, springboot, web, devops
description: Spring MVC async request timeouts are often fixed at startup. When slow downstreams hold async requests open, Kiponos lets you tighten async budgets live without a redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-mvc-async-timeout.md
---

Saturday 19:18. You went async so Tomcat would not block. Now DeferredResult handlers sit open for 60s each because spring.mvc.async.request-timeout=60000. Downstream is crawling. Async hid the pain behind a longer bill.

Someone opens the runbook and points at the frozen knob:

```
spring.mvc.async.request-timeout
```

```yaml
spring.mvc.async.request-timeout=60000
```

That value was read when the process (or client bean) was born. Changing it means a new revision while the storm is already here. The senior engineer says the sentence every on-call knows:

> "Async timeout is web framework config. Change on deploy."

You do not need a new architecture tonight. You need an **operational ceiling** that can move **now**.

**The Aha:** async request timeout is a user-visible budget, not a free lunch. With [Kiponos.io](https://kiponos.io) you hold the dials live and rebuild or re-bind from local gets — zero network on the hot path, dashboard delta in seconds, no redeploy.

## The hard-coded belief

Teams treat framework timeouts as **bootstrap philosophy**:

| What teams say | What production does |
|----------------|---------------------|
| "This number is correct forever" | Peak hours, partial outages, and partners disagree |
| "We can only change it in a release" | Incidents do not wait for CI |
| "One global value is simpler" | Checkout, catalog, and reports need different budgets |
| "Async / pool / gateway already protects us" | Each layer has a **different** clock — misalignment multiplies pain |

The pain is not missing a fancy mesh. It is treating **traffic and dependency budgets** like compile-time constants.

## What is Kiponos.io (for this incident)

[Kiponos.io](https://kiponos.io) is a live operational config hub. Your JVM opens **one WebSocket**, receives a tree snapshot for profile path `['storefront']['v2']['prod']['base']`, then **deltas only** when someone edits a key. Hot-path `getInt()` / `getLong()` reads are **local memory** — no HTTP RTT on the request path.

You keep wiring in Git (beans, routes, security). You move **operational floats** — timeouts, concurrency, cancel flags — into the hub so on-call can act without recycling pods. Secrets stay in a secret manager. Live trees hold policy, not passwords.

## Architecture

```mermaid
flowchart LR
  subgraph Ops
    D[Dashboard edit]
  end
  subgraph JVM
    K[Kiponos in-memory tree]
    H[Hot path local get]
    B[Binder / client rebuild]
  end
  D -->|WebSocket delta| K
  K --> H
  K --> B
  H --> App[Request / consumer path]
  B --> App
```

1. Connect once at startup — `Kiponos.createForCurrentTeam()` or builder with team id + access key.  
2. Snapshot loads; operational keys are already in memory.  
3. Dashboard edit → **delta** merge on a worker thread.  
4. Binder rebuilds client / refreshes container / applies session policy.  
5. Hot path keeps calling `getLong` / `getInt` locally when it needs the current budget.

## Config tree

```yaml
web_ops/
  async/
    default_timeout_ms: 10000
    endpoints/
      checkout_quote:
        timeout_ms: 5000
      export_job:
        timeout_ms: 120000
      status_poll:
        timeout_ms: 15000
    shed_on_timeout: true
```

Same JAR. Staging may use softer ceilings. Prod stays tight. A load-test profile can open budgets without copying YAML onto a laptop.

## Integration — Spring Boot 3

Bootstrap (secrets stay in env / secrets manager; **timeouts do not**):

```java
@Configuration
public class KiponosConfig {

    @Bean(destroyMethod = "disconnect")
    public Kiponos kiponos() {
        return Kiponos.createForCurrentTeam();
        // or builder: teamId, accessKey, profilePath ['storefront']['v2']['prod']['base']
    }
}
```

Live binder sketch for this knob family (`DeferredResult / WebAsyncTask`):

```java
@Component
public class LiveOpsBinder {

    private final Kiponos kiponos;
    private final AtomicReference<Long> budgetMs = new AtomicReference<>(8_000L);

    public LiveOpsBinder(Kiponos kiponos) {
        this.kiponos = kiponos;
        refresh();
        kiponos.afterValueChanged(ch -> {
            if (ch.path() != null && ch.path().contains("ops")) {
                refresh();
            }
        });
    }

    private void refresh() {
        // Read the topic tree (gateway / timelimiter / tx / kafka / lettuce / async)
        long ms = kiponos.getRootFolder()
            .folderOrCreate("edge_ops") // adapt folder to article tree
            .getLong("response_timeout_ms", 8000L);
        // Prefer typed path helpers in real code; keep hot path local
        budgetMs.set(ms);
        // rebuild client / setConcurrency / TransactionDefinition / DeferredResult supplier
    }

    public long budgetMs() {
        return budgetMs.get(); // hot path — local memory only
    }
}
```

Wire the budget into the **real** framework object for this article:

- Gateway: rebuild `HttpClient` / per-route response timeout metadata  
- TimeLimiter: rebuild `TimeLimiterConfig` + decorate supplier  
- Transactions: `DefaultTransactionDefinition#setTimeout` / `TransactionTemplate`  
- Kafka: `ConcurrentMessageListenerContainer#setConcurrency` then stop/start carefully  
- Lettuce: rebuild `ClientOptions` + swap `AtomicReference` connection  
- MVC async: construct `DeferredResult(timeoutMs)` / `WebAsyncTask` from live supplier  

## Scenarios

| Moment | Frozen reflex | Live dial |
|--------|---------------|-----------|
| Partial dependency outage | Hope + page | Tighten fail-fast budget |
| Known long report / export | Global short timeout kills work | Open only that route/use-case |
| Peak event | Guess in a war room | Pre-staged profile values |
| Load test | Hard-coded YAML | Hub `loadtest` tree |

## Before / after

| Approach | Mid-incident change | Hot path cost |
|----------|---------------------|---------------|
| YAML + redeploy | Minutes–hours | Frozen |
| Config server poll each call | Possible | Extra RTT on the hot path |
| **Kiponos** | **Seconds** | **Local get** |

## When not to use a live dial

| Case | Prefer |
|------|--------|
| Secret / cert / mTLS material | Secret manager + controlled restart |
| Topology redesign (new partitions, new routes) | Architecture + deploy |
| Auth filter logic bugs | Code fix + review |
| Unsupported language SDKs (Node, Go, .NET, …) | **Not supported** — Kiponos ships **Java (Spring Boot 2/3) and Python only** |

## Performance notes

- Rebuilds on delta are cheap compared to an outage; prefer that over polling a remote config store on every call.  
- `getInt` / `getLong` remain O(1) local memory — safe inside request and consumer loops.  
- Do not put base URLs that encode environments, passwords, or certificates in the live tree — only **operational floats**.  
- Align sibling layers (edge timeout, MVC async, client timeout, TX/SQL ceilings) so one dial does not lie about another.

## Getting started

1. Move the frozen constant(s) into a Kiponos tree with min/max guards where relevant.  
2. Bootstrap `Kiponos` once; bind refresh on `afterValueChanged`.  
3. Game day: inject latency or lag; prove the dial moves without a redeploy.  
4. Document which profile owns which dependency so on-call never edits the wrong tree.  
5. Keep GitHub as the source of truth for the essay and example links.

### Related

- [devto-aha-tomcat-threads.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-tomcat-threads.md)
- [devto-aha-gateway-response-timeout.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-gateway-response-timeout.md)
- [devto-aha-http-timeout.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-http-timeout.md)

Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io) · [GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)

---

*Kiponos.io — operational ceilings should move before the redeploy does.*
