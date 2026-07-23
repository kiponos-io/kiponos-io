---
title: "Iterator as a Super Pattern — Live Page Size"
published: false
tags: java, designpatterns, architecture, devops
description: "Page size is a knob, not a constant in a jar"
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-pattern-iterator-live-page-size.md
main_image: https://files.catbox.moe/72nynn.jpg

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

**The Aha:** Page size is a knob, not a constant in a jar

Export jobs melt under page=10000. Live page size is the difference between a bad night and a knob.

Most teams already own the Gang of Four shape. What they do not own is the **distance between human judgment and the next request**. That distance is still a release train — and that is what Super Patterns delete.

## The problem: a beautiful pattern with a frozen dial

You can draw the diagram. You can pass the interview. Production still says:

| Belief | Reality |
|--------|---------|
| "We implemented the pattern" | Selection / knobs are constants |
| "Runtime means the JVM is up" | Runtime *choice* waits for CI |
| "Flags will save us" | Another system, another delay |
| "We'll hotfix" | Customers do not wait for green builds |

I have sat in rooms where everyone agreed on the right posture and nobody could apply it without a jar. That is not engineering maturity. That is **ceremony**.

## The Aha: pattern + live policy = Super Pattern

Keep the object structure in code. Move the **dial** into [Kiponos.io](https://kiponos.io):

```yaml
patterns/iterator/paging/
  # live knobs
  page-size: 100
```

Hot path (local memory after connect — WebSocket deltas, no per-request hub RTT):

```java
Folder policy = kiponos.path('patterns', 'iterator', 'paging');
// local gets — never remote on the money path
while (it.hasNext(pageSize)) process(it.nextPage(pageSize));
```

Ops writes the hub. The next evaluation uses the new posture. Same jar. Same tests for structure. Live judgment.

## What stays versioned vs live

| Versioned (jar) | Live (hub) |
|-----------------|------------|
| Class graph / interfaces | Selection ids, enable flags |
| Algorithms / handlers | Numeric knobs, CSV lists |
| Security boundaries | Temporary posture (with process) |
| Secrets | **Never** — use a vault |

## Architecture

```text
Ops / automation ──set──▶ Kiponos.io hub
                              │ WS delta
                              ▼
                         Java SDK cache ──get──▶ pattern context
                              │
                              ▼
                         GoF structure (code)
```

## Clone and run

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-iterator-live-page-size
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full tree: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size)

Try it tonight:

1. Run tests — prove defaults are coherent.  
2. Flip the hub knobs mid-session — prove the next action sees them without rebuild.  
3. Time your real release train vs a hub write.  
4. Ask who is allowed to write this tree in production (humans, bots, both).

## Scenarios

| Moment | Frozen pattern | Super Pattern |
|--------|----------------|---------------|
| Incident posture | Hotfix PR | Hub write in seconds |
| Peak event | Over-provision and pray | Live dials |
| Experiment | Long-lived branch | Same jar, hub profile |
| Rollback | Redeploy previous | Revert hub values |

## When not to live-edit

- Protocol / schema changes that need coordinated rollouts  
- Crypto or auth that must never be optional  
- Anything your compliance process requires code review only  

Super Patterns are for **posture**, not for inventing untested systems under fire.

## Why this is not “just another flag”

Feature flags are often product gates. Super Patterns are **ops posture on a classical shape** — Strategy, Proxy, Facade, and the rest — with local reads and a single hub humans and remote SDKs share.

## Moral

Iterators walk. Super Iterators pace themselves from the hub.

Ship judgment. Leave the jar alone.

---

*Series: Kiponos Super Patterns — GoF + live policy. Intro: [Rewriting the Gang of Four](https://dev.to/kiponos/rewriting-the-gang-of-four-true-real-time-config-turns-design-patterns-into-super-patterns-nii).*

*Runnable: [pattern-iterator-live-page-size](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size) · [kiponos.io](https://kiponos.io)*


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

