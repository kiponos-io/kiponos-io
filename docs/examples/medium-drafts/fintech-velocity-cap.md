# We Redeployed a Payment Jar to Lower a Number — That Number Was Transactions Per Minute

*A traveler’s note from a fintech war room: the velocity cap that only moved through CI.*

---

I have sat through more “velocity” meetings than I care to admit.

Not the product-roadmap kind with sticky notes. The other kind — the one that starts when fraud tooling, or a partner, or a sudden flash-sale makes the transaction rate look like a stock ticker someone forgot to pause. Someone says **“cap it.”** Someone else opens a PR to change `max-tx-per-min` in a YAML file that only exists in the artifact that is currently burning money in production.

I have watched that loop in three different offices and one hotel lobby with bad Wi‑Fi:

1. Agree on a number  
2. Open a PR  
3. Wait for CI  
4. Roll a jar  
5. Discover the wrong replica still has the old number  
6. Repeat  

That is not risk control. That is a ceremony with a ledger attached.

---

## What went wrong (the human version)

The system already *had* a velocity cap. The engineers were not stupid. The problem was the **path to change it**.

In the old world, `max-tx-per-min` lived next to code:

- same repo  
- same release train  
- same “who owns the merge at 02:14?” politics  

So every real incident became a release discussion. Fraud wants **30**. Growth wants **120**. Ops wants **something now**. The jar becomes a hostage.

I once heard a lead say, half joking, half broken:

**“If we could just flip the cap without a deploy, I’d sleep.”**

That sentence is the whole product brief.

<!-- medium-img: diagram-fintech-velocity-cap-gof-vs-live.png -->

---

## The Super Pattern (live knob, not a redeploy)

The example is intentionally small. It proves one nerve:

> A running process can **read** a velocity limit from a live hub, and ops can **set** that limit without rebuilding the payment jar.

Hub path:

```text
examples/fintech-velocity-cap/max-tx-per-min = 60
```

Local `get()` on the hot path (or a short-lived cache with a live invalidation story). Dashboard or remote SDK `set()` when the world changes. Same tree for every replica. No “which box still has the old YAML?” scavenger hunt.

<!-- medium-img: diagram-fintech-velocity-cap-hub-flow.png -->

---

## The example (standalone Java)

Published under:

**`examples/java/fintech-velocity-cap`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/fintech-velocity-cap)

It connects with `Kiponos.createForCurrentTeam()`, ensures the folder, seeds a safe default, and prints the live cap:

```java
public static void main(String[] args) throws Exception {
    Kiponos k = Kiponos.createForCurrentTeam();
    try {
        Folder p = ensure(k);
        int cap = readInt(p, "max-tx-per-min", 60);
        System.out.println("max-tx-per-min=" + cap);
        // hot path: refuse new work when rate would exceed cap
        Thread.sleep(1500L);
    } finally {
        k.disconnect();
    }
}

static Folder ensure(Kiponos k) {
    Folder f = k.getRootFolder()
        .folderOrCreate("examples")
        .folderOrCreate("fintech-velocity-cap");
    if (!f.hasKey("max-tx-per-min")) {
        f.set("max-tx-per-min", "60");
    }
    return f;
}

static String read(Folder p, String key, String def) {
    if (!p.hasKey(key)) {
        return def;
    }
    String r = p.get(key);
    return r == null || r.isBlank() ? def : r.trim();
}

static int readInt(Folder p, String key, int def) {
    try {
        return Integer.parseInt(read(p, key, String.valueOf(def)));
    } catch (Exception e) {
        return def;
    }
}
```

Ops changes the number on the hub. You re-run (or keep the process listening). **No jar rebuild** to lower the rate while fraud cools down.

---

## How to try it

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/fintech-velocity-cap
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Then flip `max-tx-per-min` on the Kiponos dashboard (or another SDK client) and run again. The printed value should follow the hub — not the last release.

Full source + tests:  
https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/fintech-velocity-cap

---

## Old world vs live hub

| Move | Old world | Live hub |
|------|-----------|----------|
| Change the cap | PR → CI → roll | Dashboard / SDK `set()` |
| Wrong replica | Drift until next deploy | Same tree, WebSocket fan-out |
| Incident rollback | Redeploy previous artifact | Flip the value back |
| Audit story | “Who merged?” | Who set the key, when, why |

---

## The moral

**People should not have to ship a release to make a decision.**

A velocity cap is a judgment call under pressure. Judgment belongs on a live path with a name humans can find at 3am — not buried in the next green build.

Ship the judgment path once. Leave the jar alone when the only thing that changed is a number.

---

## Operational checklist (keep this boring)

1. Name the hub path so humans find it under pressure.  
2. Default safely when the hub is unreachable (fail closed or fail to last-known-good — pick on purpose).  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision** (from → to + reason), not every `get`.  
5. Rehearse the flip in staging before the next flash sale.  
6. Document the one-line kill path (raise or lower the cap).  
7. Put that line in the incident timeline template.

Boring checklists survive 3am. Clever ones do not.

---

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/fintech-velocity-cap](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/fintech-velocity-cap)*
