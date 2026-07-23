# The Config Server Blinked — and the App Should Not Have Died With It

*A traveler’s note from last-known-good, fail-closed defaults, and the night “high availability” still left you without a number.*

---

There is a particular kind of outage that feels unfair.

Your payment path is fine. Your database is fine. Your team is awake and competent. Then the **config hub** hiccups — a blip, a TLS glitch, a bad deploy on the control plane — and every process that treated remote config as a remote *call* becomes a 503 factory.

I have heard the postmortem sentence more than once:

**“We were highly available… except for config.”**

That sentence should embarrass architecture diagrams.

I have sat in war rooms where the product was healthy and the **memory of the last good timeout** would have been enough. We did not have that memory. We had stack traces. Airports taught me that screens go blank while planes still exist. On-call taught me that **last known good is not a luxury — it is a moral minimum** when money is in flight.

---

## The lie we tell about “remote config”

We teach juniors that hard-coded constants are bad.

So they replace constants with remote fetches. Every request. Every timeout. Every feature flag. Then we call it cloud-native and go to lunch.

Old world under a blip:

1. Hub unreachable  
2. Client library throws  
3. Hot path has no number to use  
4. You fail open into chaos **or** fail closed into a total outage  
5. Either way, you learn that **availability of truth** was never designed  

The jar was fine. The network was not. The design assumed the control plane would never blink.

---

## Live / LKG / Safe — three postures, one hot path

Kiponos is not “call the server for every timeout.” It is **hub + WebSocket + in-process cache**, with a posture layer when the network lies.

Three modes your hot path should understand:

- **Live** — hub reachable, cache fresh. Serve the current hub value. Normal ops.  
- **LKG (last known good)** — hub blipped, cache still warm. Serve the **last good number**. Keep money moving.  
- **Safe** — no trusted memory left. Serve a **conservative default**. Fail closed, not NPE.

Who owns what:

- **Kiponos.io hub** — source of truth when online  
- **WebSocket deltas** — patch the in-memory tree without polling  
- **SDK cache** — hot-path `.getInt()` with **no network RTT**  
- **Your posture policy** — choose Live vs LKG vs Safe when the wire is weird  

<!-- medium-img: diagram-lkg-posture.png -->

This is not “just Redis.” A remote GET on every authorize() dies with the hub. Static YAML is free on the hot path and stale forever. **SDK cache + LKG** keeps a number in local memory and serves last good until Safe — without waiting for a redeploy to invent honesty.

---

## The demo tree

```text
examples / api-10-offline-lkg-reads /
  payment-timeout-ms  = int
```

What the example does under each hub state:

- **Live** — serve `payment-timeout-ms` from hub/cache  
- **Offline + LKG held** — serve the same last good integer  
- **Safe** — serve a conservative default (fail closed)  

[Kiponos](https://kiponos.io) keeps the in-process cache. This example is the **policy layer** around it: which number do humans still trust when the network lies?

---

## The example

**`examples/java/api-10-offline-lkg-reads`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

Standalone Java. Pure posture resolver, unit-tested without mocking WebSockets.

```bash
cd examples/java/api-10-offline-lkg-reads
export KIPONOS_ID='…'
export KIPONOS_ACCESS='…'
./gradlew test run
```

Try it tonight:

1. Run the tests. Confirm Live, LKG, and Safe each return a **defined** integer — never a coin flip.  
2. Change `payment-timeout-ms` in the hub. Re-run. Watch Live pick up the new value.  
3. Imagine the hub gone with a warm cache — LKG should still hand you the last good timeout.  
4. Imagine the hub gone with no trusted memory — Safe should fail closed, not NPE the payment path.  

Ops play: if your design has no number when the control plane blinks, you did not build resilience. You built a single point of failure with better marketing.

---

## The moral

If your app dies when the config server blinks, you did not build a distributed system.

**Ship last-known-good as product behavior**, not as an incident regret.

People should not have to ship a release to keep a timeout honest.

---

*Example + tests: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/api-10-offline-lkg-reads](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/api-10-offline-lkg-reads)*
