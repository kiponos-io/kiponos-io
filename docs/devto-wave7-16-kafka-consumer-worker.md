---
title: "The Consumer Lag Was Real — and Redeploy Was Not a Pause Button"
published: true
tags: kafka,java,devops,microservices
description: "A traveler’s note from poison messages, growing lag graphs, and the worker knobs that should not wait for CI."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/examples/medium-drafts/16-kafka-consumer-worker.md
main_image: https://files.catbox.moe/tngzui.jpg
---

*A traveler’s note from poison messages, growing lag graphs, and the worker knobs that should not wait for CI.*

---

There is a particular graph that makes on-call people stop joking.

Not a red 500 rate. Not a disk full alert. Lag.

The line that climbs while the room argues about whether the consumer is “just slow” or “actually stuck.” Someone opens a terminal. Someone opens Grafana. Someone says the sentence I have heard in three time zones:

**“Just pause the consumer group. We need to dig.”**

And then someone else says the worse sentence:

**“We’d have to redeploy with `paused=true`.”**

Redeploy.

To pause.

While lag is a business problem and poison messages are a moral one.

---

## Architecture: what “live pause” actually looks like

Kiponos sits **beside** the worker, not on the Kafka hot path as a remote call per poll:

![Kafka worker live control plane](https://litter.catbox.moe/zsnu1f.png)

| Layer | Role | Latency on the poll loop |
|-------|------|---------------------------|
| **Kiponos dashboard** | Ops flips `paused` / `prefetch` / `max-poll-records` | Human decision |
| **Kiponos.io hub** | Holds the typed tree; fans out deltas | — |
| **Java SDK in-memory cache** | WebSocket merges; local `.get()` | **Zero RTT** |
| **Poll loop** | Reads policy from memory, then talks to Kafka | Only Kafka I/O |

That is the design point people miss: **control plane ≠ data plane.** You do not call the hub on every `poll()`. You call local memory that was already updated when ops moved the knob.

### Old world vs Kiponos

| Question | Old world (YAML / Helm) | With Kiponos |
|----------|-------------------------|--------------|
| Pause consumer | PR → CI → roll pods | Dashboard `paused=yes` |
| Shrink batch under poison | Redeploy `max.poll.records` | Live `max-poll-records` |
| Tune prefetch | Hope the next chart is green | Live `prefetch` |
| Wrong replica still running old flags | Common | Same JAR, same tree, WebSocket fan-out |

---

## Configuration hell wears a Kafka hoodie

We tell ourselves that stream processing is modern.

We have consumer groups and rebalances and beautiful dashboards. Then a bad payload lands — one bad schema, one null where a decimal should be — and the worker spins on the same offset like a tourist who will not leave the baggage carousel.

Old world theater:

1. Edit `application.yml`  
2. PR titled `temp: pause orders-consumer`  
3. Wait for CI like it is weather  
4. Deploy  
5. Discover one pod is still on yesterday’s artifact  
6. Merge a “revert the temporary pause” three days later that nobody reviews carefully  

I have watched excellent platform engineers — people who can reason about exactly-once without notes — reduced to archaeologists of their own Helm values because **pause was packaged as a release**.

Airports taught me that gates renumber without apology. Kafka taught me that a temporary YAML flag is a confession: **your control plane is your git history.**

---

## The worker tree (design, not decoration)

A real worker has a small family of related moves under one mental folder:

| Key | Type | Meaning |
|-----|------|---------|
| `paused` | bool-ish (`yes`/`no`) | May the worker pull at all? |
| `prefetch` | int | How hungry is the fetch window? |
| `max-poll-records` | int | How big is each poll batch when the dependency limps? |

Tree shape in the hub:

```text
examples / 16-kafka-consumer-worker /
  paused            = yes | no
  prefetch          = int
  max-poll-records  = int
```

| Effective state | Behavior |
|-----------------|----------|
| `paused=yes` | Do not pull — freeze the poison storm; lag may grow on purpose |
| running | Honor prefetch / max-poll (**prefetch clamped ≥ poll batch**) |

Old world: those answers live in three files, two env overlays, and a wiki page last edited during a previous poison storm.

New world:

1. Open the [Kiponos](https://kiponos.io) hub  
2. Set `paused` to `yes` (or lower `max-poll-records`)  
3. The process adopts a safer posture **without** a parade of pull requests  

Judgment at the speed of the person who is already awake.

---

## The example (standalone Java)

**`examples/java/16-kafka-consumer-worker`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

Plain `main`. No Spring ceremony. No “config refresh endpoint” you will forget to secure at 4am.

```bash
cd examples/java/16-kafka-consumer-worker
export KIPONOS_ID='…'
export KIPONOS_ACCESS='…'
./gradlew test run
```

Then in the hub: set `paused=yes`, re-run, watch the posture line change. Logic tests ship with the example so CI proves the clamps without a live cluster.

---

## The moral

Consumer lag is a **control-plane problem** dressed up as a data problem.

If pausing a worker requires a release train, you do not have a pause button. You have a prayer with a pipeline.

Ship the judgment. Leave the jar alone.

---

*Example + tests: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/16-kafka-consumer-worker](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/16-kafka-consumer-worker)*
