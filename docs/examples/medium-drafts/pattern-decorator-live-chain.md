# The Decorator Stack Was Elegant — and Completely Frozen

*A traveler’s note on nested wrappers, retry storms, and the Super Pattern that lets ops rebuild the chain without a jar.*

---

I have always liked the Decorator pattern on a whiteboard.

It looks like craftsmanship: a core call wrapped in metrics, then retry, then cache — each layer polite, each layer testable. Someone says **“we can add behavior without touching the core.”** Everyone nods. The diagram looks like a necklace of responsibility. You feel smart for drawing it.

Then production lights up. A dependency starts timing out. Retries amplify the dying peer. Cache starts returning stale “success.” Metrics scream. And the only way to *remove* a layer from that elegant stack is a release train — the same train that put the stack there in the first place.

That is the moment Decorator stops feeling like freedom and starts feeling like concrete that dried while we were still arguing about the pour.

---

## What Decorator was trying to buy us

Composition. Freedom to stack cross-cutting concerns without a god class. A clean core. Wrappers that each do one job.

What it did not buy us: a place to store **tonight’s stack** while the process is already hot.

In most Java services the chain is decided at wiring time — Spring beans, Guice modules, a `new Retrying(new Caching(new Metrics(core)))` in a factory. Once the JVM is up, the necklace is welded. You can change knobs *inside* a layer if you were careful (retry max, cache TTL). You cannot easily drop the retry layer itself when retries are the fire.

---

## Super Pattern: Live Decorator Chain

Same idea — wrap a core. Different nervous system.

```text
patterns / decorator / http-client / chain
patterns / decorator / http-client / retry-max
patterns / decorator / http-client / cache-ttl-s
```

- `chain` is a CSV allowlist: `metrics`, `retry`, `cache`  
- Each call rebuilds the pipeline from the **local** Kiponos tree  
- Dashboard or remote SDK can drop retry mid-incident  
- Layer-local knobs (`retry-max`, `cache-ttl-s`) still work — and still update live  

**Structure stays GoF. Selection of layers becomes operational.**

You keep the mental model of Decorator. You stop pretending the only legal time to edit the stack is a deploy window.

<!-- medium-img: diagram-decorator-stack.png -->

---

## The night retries were the problem

I have been in rooms where the dependency was already dead and the client was still polite: three retries, exponential backoff, “just being resilient.” Resilience that cannot be switched off is not resilience. It is a thrash amplifier with good intentions.

With a live chain, ops can set:

```text
chain = metrics,cache
```

and leave retry on the floor until the peer recovers. No PR. No rebuild. No “we’ll ship a hotfix after the war room.”

That is not a silver bullet for bad architecture. It is a fire extinguisher for a class of outages we invent ourselves by freezing composition into the jar.

---

## The example

```bash
examples/java/pattern-decorator-live-chain
./gradlew test run
```

It prints the active chain and a trace of which wrappers ran. Change `chain` in the hub. Run again. The jar is innocent.

Flip `retry-max` while the process is up. Drop `cache` for one incident window. Put it back when the smoke clears. The Decorator *shape* never changed. The *membership* of the stack did — at operational speed.

<!-- medium-img: diagram-decorator-hub.png -->

---

## What not to do

Do not put secrets in the chain list. Do not use live layers as a substitute for fixing a flaky dependency. Do not hide business rules inside a stack of wrappers that only three people understand.

Do use a live chain when the cost of a wrong stack *tonight* is higher than the cost of a controlled live edit.

---

## The moral

Decorator always promised composition.

**Kiponos makes the composition list something you can change at the speed of an outage — not the speed of CI.**

---

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-decorator-live-chain](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-decorator-live-chain)*
