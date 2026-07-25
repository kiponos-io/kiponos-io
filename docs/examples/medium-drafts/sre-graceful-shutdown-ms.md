# Graceful Shutdown Window Live

*A traveler’s note: drain window live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

drain window live

Redeploying a jar to change `drain-ms` is how teams invent 3am folklore.

---

## Hub tree

```text
examples/sre-graceful-shutdown-ms/drain-ms = 15000
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("drain-ms=" + read(p, "drain-ms", "15000"));
            System.out.println("drain window live");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("sre-graceful-shutdown-ms");
        if (!f.hasKey("drain-ms")) {
            f.set("drain-ms", "15000");
        }
        return f;
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) {
            return def;
        }
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/sre-graceful-shutdown-ms
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms)

This article only shows the nerve. The repo is the product.

---

## Old world vs Kiponos

| Move | Old world | Live hub |
|------|-----------|----------|
| Change the knob | PR → CI → roll | Dashboard / SDK `set()` |
| Wrong replica | Drift | Same tree, WebSocket fan-out |
| Incident rollback | Redeploy previous | Flip the value back |

---

## The moral

**People should not have to ship a release to make a decision.**

Ship the judgment path once. Leave the jar alone.

---

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-graceful-shutdown-ms)*

<!-- medium-img: diagram-sre-graceful-shutdown-ms-gof-vs-live.png -->
<!-- medium-img: diagram-sre-graceful-shutdown-ms-hub-flow.png -->


---

## Why this still matters on a quiet Tuesday

Incidents train the muscle. Quiet days keep it honest.

If `drain-ms` only moves through a release train, every product conversation becomes a ceremony debate: who owns the PR, who merges, who rolls, who watches. That tax compounds across regions and on-call rotations.

Live posture is not a license for chaos. It is a contract:

- named path humans can find under pressure  
- clamps that survive panic  
- audit that names the actor  
- a one-line revert that does not require a hero  

When those four exist, the Super Pattern stops being a demo and becomes how the system grows older without growing brittle.


## Operational checklist (keep this boring)

1. Name the hub path so humans find it under pressure.  
2. Default safely when the hub is unreachable.  
3. Allowlist writers (dashboard roles + automation identities).  
4. Log the **decision**, not every get.  
5. Rehearse the flip in staging.  
6. Document the one-line kill path (revert key).  
7. Record from→to + reason code in the incident timeline.

Boring checklists survive 3am. Clever ones do not.

