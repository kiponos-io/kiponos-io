# The Decorator Stack Was Elegant — and Completely Frozen

*A traveler’s note on nested wrappers, retry storms, and the Super Pattern that lets ops rebuild the chain without a jar.*

---

I have always liked the Decorator pattern on a whiteboard.

It looks like craftsmanship: a core call wrapped in metrics, then retry, then cache — each layer polite, each layer testable. Someone says **“we can add behavior without touching the core.”** Everyone nods.

Then production lights up, retries amplify a dying dependency, and the only way to *remove* a layer is a release train.

---

## What Decorator was trying to buy us

Composition. Freedom to stack cross-cutting concerns without a god class.

What it did not buy us: a place to store **tonight’s stack** while the process is already hot.

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

**Structure stays GoF. Selection of layers becomes operational.**

<!-- medium-img: diagram-decorator-stack.png -->

---

## The example

```bash
examples/java/pattern-decorator-live-chain
./gradlew test run
```

It prints the active chain and a trace of which wrappers ran. Change `chain` in the hub. Run again. The jar is innocent.

<!-- medium-img: diagram-decorator-hub.png -->

---

## Moral

Decorator always promised composition.

**Kiponos makes the composition list something you can change at the speed of an outage — not the speed of CI.**
