# Abstract Factory as a Super Pattern — Region Family Live

*A traveler’s note: US vs EU product family without redeploy.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

US vs EU product family without redeploy

Redeploying a jar to change `family` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/abstract-factory/region/family = us
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws InterruptedException {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensure(k);
            RegionFactory f = createFamily(policy);
            System.out.println("========================================");
            System.out.println("  Super Pattern: Live Abstract Factory");
            System.out.println("  family:   " + f.family());
            System.out.println("  currency: " + f.currency().code());
            System.out.println("  tax:      " + f.tax().label());
            System.out.println("========================================");
            Thread.sleep(2000L);
        } finally { k.disconnect(); }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("abstract-factory").folderOrCreate("region");
        if (!f.hasKey("family")) f.set("family", "us");
        return f;
    }

    static RegionFactory createFamily(Folder policy) {
        String fam = read(policy, "family", "us").toLowerCase(Locale.ROOT);
        return "eu".equals(fam) ? new EuFactory() : new UsFactory();
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-abstract-factory-live-region
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-abstract-factory-live-region](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-abstract-factory-live-region)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-abstract-factory-live-region](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-abstract-factory-live-region)*

<!-- medium-img: diagram-pattern-abstract-factory-live-region-gof-vs-live.png -->
<!-- medium-img: diagram-pattern-abstract-factory-live-region-hub-flow.png -->


---

## Why this still matters on a quiet Tuesday

Incidents train the muscle. Quiet days keep it honest.

If `family` only moves through a release train, every product conversation becomes a ceremony debate: who owns the PR, who merges, who rolls, who watches. That tax compounds across regions and on-call rotations.

Live posture is not a license for chaos. It is a contract:

- named path humans can find under pressure  
- clamps that survive panic  
- audit that names the actor  
- a one-line revert that does not require a hero  

When those four exist, the Super Pattern stops being a demo and becomes how the system grows older without growing brittle.


