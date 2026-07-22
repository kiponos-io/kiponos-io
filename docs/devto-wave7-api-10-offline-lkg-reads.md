---
title: "The Config Server Blinked — and the App Should Not Have Died With It"
published: true
tags: java,devops,reliability,architecture
description: "A story about last-known-good, fail-closed defaults, and the night “high availability” meant your timeout still had a number."
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/examples/medium-drafts/api-10-offline-lkg-reads.md
main_image: https://files.catbox.moe/tngzui.jpg
---

*A story about last-known-good, fail-closed defaults, and the night “high availability” meant your timeout still had a number.*

---

There is a special kind of outage that feels unfair.

Your payment path is fine. Your database is fine. Your team is awake and competent. Then the **config hub** hiccups — a blip, a TLS glitch, a bad deploy on the control plane — and suddenly every process that treated remote config as a remote *call* becomes a 503 factory.

I have heard the postmortem phrase more than once:

**“We were highly available… except for config.”**

That sentence should embarrass architecture diagrams.

---

## Architecture: Live / LKG / Safe

Kiponos is not “call the server for every timeout.” It is **hub + WebSocket + in-process cache**, with a posture layer when the network lies:

![Live, LKG, and Safe posture around the SDK cache](https://litter.catbox.moe/qgm9di.png)

| Mode | When | What the hot path gets | Design intent |
|------|------|------------------------|---------------|
| **Live** | Hub reachable; cache fresh | Current hub value | Normal ops |
| **LKG** | Hub blip; cache still warm | **Last known good** number | Keep money moving |
| **Safe** | No trusted memory | Conservative default | Fail closed, not NPE |

| Layer | Responsibility |
|-------|----------------|
| **Kiponos.io hub** | Source of truth when online |
| **WebSocket deltas** | Patch the in-memory tree without polling |
| **SDK cache** | Hot-path `.getInt()` with **no network RTT** |
| **Your posture policy** | Choose Live vs LKG vs Safe when the wire is weird |

### Why this is not “just Redis”

| Approach | Hot-path cost | Offline behavior |
|----------|---------------|------------------|
| Remote GET every authorize() | Network + failure modes | Dies with the hub |
| Static YAML | Zero | Stale forever; redeploy to change |
| **Kiponos SDK cache + LKG** | Local memory | Serves last good until Safe |

---

## Configuration hell, network edition

We teach juniors that “hard-coded constants are bad.”

So they replace constants with remote fetches. Every request. Every timeout. Every feature flag. Then we call it cloud-native and go to lunch.

Old world under a blip:

1. Hub unreachable  
2. Client library throws  
3. Hot path has no number to use  
4. You fail open into chaos **or** fail closed into a total outage  
5. Either way, you learn that **availability of truth** was never designed  

I have sat in war rooms where the product was healthy and the **memory of the last good timeout** would have been enough. We did not have that memory. We had stack traces.

Airports taught me that screens go blank while planes still exist. On-call taught me that **last known good is not a luxury — it is a moral minimum** when money is in flight.

---

## The demo tree

```text
examples / api-10-offline-lkg-reads /
  payment-timeout-ms  = int
```

| Hub state | Policy in the example |
|-----------|------------------------|
| Live | Serve `payment-timeout-ms` from hub/cache |
| Offline + LKG held | Serve the same last good integer |
| Safe | Serve a conservative default (fail closed) |

[Kiponos](https://kiponos.io) keeps the in-process cache. This example is the **policy layer** around it: which number do humans still trust when the network lies?

---

## The example

**`examples/java/api-10-offline-lkg-reads`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

Standalone Java. Pure posture resolver unit-tested without mocking WebSockets.

```bash
cd examples/java/api-10-offline-lkg-reads
export KIPONOS_ID='…'
export KIPONOS_ACCESS='…'
./gradlew test run
```

Ops play: change `payment-timeout-ms` in the hub, re-run. Imagine the hub gone — your design should still have a number that is not a coin flip.

---

## The moral

If your app dies when the config server blinks, you did not build a distributed system.

You built a single point of failure with better marketing.

**Ship last-known-good as product behavior**, not as an incident regret.

---

*Example + tests: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/api-10-offline-lkg-reads](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/api-10-offline-lkg-reads)*
