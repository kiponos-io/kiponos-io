# The Decorator Stack Was Elegant — and Completely Frozen

*A traveler’s note from a dependency death spiral: the wrapper chain that only rewired through CI.*

---

I have always liked the Decorator pattern on a whiteboard.

It looks like craftsmanship: a core call wrapped in metrics, then retry, then cache — each layer polite, each layer testable. Someone says **“we can add behavior without touching the core.”** Everyone nods. The diagram looks like a necklace of responsibility. You feel smart for drawing it.

Then production lights up. A dependency starts timing out. Retries amplify the dying peer. Cache starts returning stale “success.” Metrics scream. And the only way to *remove* a layer from that elegant stack is a release train — the same train that put the stack there in the first place.

That is the moment Decorator stops feeling like freedom and starts feeling like concrete that dried while we were still arguing about the pour.

I have watched that loop in three different offices and one war room with cold coffee:

1. Agree the stack is wrong for *tonight*  
2. Open a PR to drop retry (or cache, or both)  
3. Wait for CI  
4. Roll a jar  
5. Discover half the fleet still has the old necklace  
6. Repeat while the peer stays dead  

That is not resilience. That is a ceremony with a thrash amplifier attached.

---

## What went wrong (the human version)

Decorator bought us composition. Freedom to stack cross-cutting concerns without a god class. A clean core. Wrappers that each do one job.

What it did **not** buy us: a place to store **tonight’s stack** while the process is already hot.

In most Java services the chain is decided at wiring time — Spring beans, Guice modules, a `new Retrying(new Caching(new Metrics(core)))` in a factory. Once the JVM is up, the necklace is welded. You can change knobs *inside* a layer if you were careful (retry max, cache TTL). You cannot easily drop the retry layer itself when retries are the fire.

I once heard an on-call engineer say, half joking, half broken:

**“If we could just take retry off the stack without a deploy, I’d sleep.”**

That sentence is the whole product brief.

<!-- medium-img: diagram-decorator-stack.png -->

---

## The Super Pattern (live chain, not a redeploy)

Same idea — wrap a core. Different nervous system.

```text
patterns / decorator / http-client / chain       = metrics,retry,cache
patterns / decorator / http-client / retry-max   = 2
patterns / decorator / http-client / cache-ttl-s = 30
```

- `chain` is a CSV allowlist of layers  
- Each call rebuilds the pipeline from the **local** Kiponos tree  
- Dashboard or remote SDK can drop retry mid-incident  
- Layer-local knobs (`retry-max`, `cache-ttl-s`) still work — and still update live  

**Structure stays GoF. Membership of the stack becomes operational.**

You keep the mental model of Decorator. You stop pretending the only legal time to edit the stack is a deploy window.

<!-- medium-img: diagram-decorator-hub.png -->

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

## The example (standalone Java)

Published under:

**`examples/java/pattern-decorator-live-chain`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-decorator-live-chain)

It connects with `Kiponos.createForCurrentTeam()`, ensures the policy folder, builds the stack from the live CSV, and prints which wrappers ran:

```java
static CallResult execute(Folder policy, String path) {
    List<String> chain = parseChain(read(policy, "chain", "metrics,retry"));
    int retryMax = readInt(policy, "retry-max", 2);
    int cacheTtl = readInt(policy, "cache-ttl-s", 30);

    UnaryOperator<Request> pipeline = core;
    // listed order is outer→inner left-to-right
    for (int i = chain.size() - 1; i >= 0; i--) {
        pipeline = wrap(chain.get(i), pipeline, trace, retryMax, cacheTtl);
    }
    Request out = pipeline.apply(new Request(path, null));
    return new CallResult(String.join(",", chain), out.body(), trace);
}

static Folder ensureDecoratorFolder(Kiponos kiponos) {
    Folder http = kiponos.getRootFolder()
        .folderOrCreate("patterns")
        .folderOrCreate("decorator")
        .folderOrCreate("http-client");
    if (!http.hasKey("chain")) {
        http.set("chain", "metrics,retry");
    }
    return http;
}
```

Ops changes `chain` on the hub. You re-run (or keep the process listening). **No jar rebuild** to drop the layer that is making the fire worse.

---

## How to try it

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-decorator-live-chain
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Then flip `patterns/decorator/http-client/chain` on the Kiponos dashboard (or another SDK client) and run again. The printed trace should follow the hub — not the last release.

Full source + tests:  
https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-decorator-live-chain

---

## What stays versioned vs live

| Versioned in the jar | Live in the hub |
|----------------------|-----------------|
| Core call + layer implementations | `chain` membership CSV |
| Allowlist of layer names | `retry-max`, `cache-ttl-s` |
| Safe defaults when hub is down | Tonight’s stack for this incident |
| Pure wrapper logic | Whether retries are allowed to thrash |

Misplace a row and you either freeze the stack or invite unreviewed composition into production via a free-text box.

---

## What not to do

Do not put secrets in the chain list. Do not use live layers as a substitute for fixing a flaky dependency. Do not hide business rules inside a stack of wrappers that only three people understand.

Do use a live chain when the cost of a wrong stack *tonight* is higher than the cost of a controlled live edit.

---

## War-room protocol (keep this boring)

1. Name the hub path: `patterns/decorator/http-client/*`  
2. Speak the allowlist before anyone types (`metrics`, `retry`, `cache` only)  
3. Write the reason code with the change (`peer-down`, `cache-poison`, `drill`)  
4. Watch error rate and peer load for five minutes  
5. Put the dropped layer back when the smoke clears — never leave a “temporary” chain as the silent default  
6. Postmortem line: who moved `chain`, from→to, whether automation should own the next drop  

Boring checklists survive dependency death. Clever ones do not.

---

## The moral

**People should not have to ship a release to make a decision.**

Decorator always promised composition. Composition that only rewires at CI speed is not composition — it is concrete with a nice diagram.

Ship the chain path once. Leave the jar alone when the only thing that changed is **which wrappers should run next**.

---

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-decorator-live-chain](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-decorator-live-chain)*
