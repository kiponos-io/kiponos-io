---
main_image: https://files.catbox.moe/tmn9l0.jpg
title: "Your Decorator Stack Was Compiled-In — We Made the Wrapper Chain Live (Kiponos Super Patterns)"
published: false
tags: java, designpatterns, architecture, devops
description: GoF Decorator promises dynamic wrapping. Most stacks are still DI-time fossils. Kiponos Super Pattern — live chain CSV and knobs humans and remote SDKs can change without redeploy.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-super-pattern-decorator.md

## Operational checklist

Before you electrify a pattern in production:

1. **Name the hub path** so humans can find it under pressure (`patterns/...`).  
2. **Default safely** — cold start without the hub still works (fail closed where money is involved).  
3. **Allowlist writers** — who can `set()` this tree (dashboard roles, automation identities).  
4. **Log the effective value** on decision points (not every get — the decision).  
5. **Rehearse the flip** in staging with the same example module you ship in the article.  
6. **Document the kill path** — how to revert hub values in one sentence.

If you skip the checklist, you did not build a Super Pattern. You built a remote foot-gun.

## Related reading

- [Rewriting the Gang of Four](https://dev.to/kiponos/rewriting-the-gang-of-four-true-real-time-config-turns-design-patterns-into-super-patterns-nii)  
- [Strategy selection live](https://dev.to/kiponos/the-strategy-pattern-still-required-a-deploy-until-we-made-selection-live-kiponos-super-patterns-1dgm)  
- Getting started: [GitHub GETTING-STARTED](https://github.com/kiponos-io/kiponos-io/blob/master/GETTING-STARTED.md)


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



## Try it tonight

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-decorator-live-chain
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

1. Run with default chain — prove metrics/retry path.  
2. Set hub `chain=metrics` only — kill retry amplification without a rebuild.  
3. Set `cache-ttl-s` low for a sale window — wrong TTL is a business lie until you can fix it live.  
4. Keep auth **outside** the live list if it must never be optional.

## Moral

Decorator is composition. Super Decorator is **composition you can still change after the jar left the building**.

If your wrapper order is a fossil, you do not have dynamic wrapping — you have a nested constructor.


*Kiponos.io — composition lists are policy, not fossils.*

## Why this is not “just another flag”

Feature flags are often product gates. Super Patterns are **ops posture on a Gang of Four shape**.

You still allowlist keys. You still test defaults. You still refuse secrets in the hub. What changes is the **distance between human judgment and the next request** — from a release train to a hub write.

That is the entire point of electrifying the patterns.


## A note on testing Super Patterns

Unit-test the **structure** with fixed strings (no network). Integration-test the **hub path** against the public sandbox when you can.

Good tests:

- Defaults when keys are missing  
- Allowlisted values only (reject unknown provider / step ids)  
- Fail-closed behavior for money paths  

Bad tests:

- Hitting production hubs from CI  
- Asserting wall-clock times for WebSocket delivery  

The example modules under `examples/java/pattern-*` are meant to be the golden path — clone them before inventing a second design.

## Closing

If you only remember one sentence: **patterns organize code; Super Patterns organize judgment.**

The jar is for what is true across deploys. The hub is for what must be true in the next minute.

