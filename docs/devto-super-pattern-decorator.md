---
main_image: https://litter.catbox.moe/zghigy.jpg
title: "Your Decorator Stack Was Compiled-In — We Made the Wrapper Chain Live (Kiponos Super Patterns)"
published: false
tags: java, designpatterns, architecture, devops
description: GoF Decorator promises dynamic wrapping. Most stacks are still DI-time fossils. Kiponos Super Pattern — live chain CSV and knobs humans and remote SDKs can change without redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-super-pattern-decorator.md
---

**The Aha:** Decorator is not the hard part. **Which wrappers are on tonight** is. Store the chain as a live list in [Kiponos.io](https://kiponos.io); the next call rebuilds the stack — metrics, retry, cache — without a release.

## The problem: “dynamic” wrappers frozen at bean creation

Every clean architecture diagram shows nested boxes:

```text
Client → Metrics → Retry → Cache → CoreHttp
```

That picture is a **story about composition**. Production still ships the order in `@Bean` methods and constructor lists. When SRE wants metrics only (no retry storms) or merchandising wants a short cache TTL, the stack is a fossil until the next jar.

| Belief | Production |
|--------|------------|
| “We use Decorators, so we are flexible” | Wrapper order is compile/DI time |
| “Toggle features with flags” | Flag often still needs a deploy path |
| “Retry is reliability” | Retry can *cause* the outage |
| “Cache is performance” | Wrong TTL is a business lie |

## The Aha: live decorator chain = Super Pattern

Keep the GoF idea — wrap a core component. Move the **composition list** and knobs into Kiponos:

```yaml
patterns/
  decorator/
    http-client/
      chain: metrics,retry        # csv: metrics | retry | cache
      retry-max: 2
      cache-ttl-s: 30
```

On each call:

```java
List<String> chain = parseChain(policy.get("chain")); // local get
UnaryOperator<Request> pipeline = core;
for (int i = chain.size() - 1; i >= 0; i--) {
    pipeline = wrap(chain.get(i), pipeline, /* knobs from hub */);
}
return pipeline.apply(request);
```

Ops sets `chain` to `metrics,cache` during a retry storm. A remote SDK can append `cache` for a sale window. WebSocket deltas update the in-memory tree; **next** request builds a new stack. Core HTTP code never changes.

## Architecture

```text
Dashboard / remote SDK
        │  set chain=metrics,cache
        ▼
  Kiponos in-process tree ──get──▶ buildWrappers()
                                        │
                    ┌───────────────────┼───────────────────┐
                 Metrics              Cache               Core
```

## Runnable example

[examples/java/pattern-decorator-live-chain](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-decorator-live-chain)

```bash
cd examples/java/pattern-decorator-live-chain
./gradlew test run
```

## Scenarios

| Moment | Frozen stack | Super Pattern |
|--------|--------------|---------------|
| Retry amplifies outage | Redeploy without retry bean | `chain=metrics` only |
| Flash sale needs short cache | PR for TTL | `cache-ttl-s=15` + `chain=…,cache` |
| Partner SDK enables tracing | Ticket to platform | Remote `set("chain", "metrics,retry,cache")` |

## When not

| Case | Why |
|------|-----|
| Security wrapper must never be optional | Keep auth *outside* the live list (always applied) |
| Arbitrary class names in the CSV | Allowlist layer ids only |

## Series

Part of **Kiponos Super Patterns** — electrifying Gang of Four so runtime freedom is real for humans **and** remote SDKs.

- Overview: [Rewriting the Gang of Four…](https://dev.to/kiponos/rewriting-the-gang-of-four-true-real-time-config-turns-design-patterns-into-super-patterns-2k0)  
- Strategy: [Strategy selection live](https://dev.to/kiponos/the-strategy-pattern-still-required-a-deploy-until-we-made-selection-live-kiponos-super-patterns-1dgm)

---

*Kiponos.io — composition lists are policy, not fossils.*
