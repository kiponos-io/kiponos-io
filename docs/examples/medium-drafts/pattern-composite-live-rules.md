# Composite Super Pattern — Node Weights Live

*A traveler’s note: composite scoring weights live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

composite scoring weights live

Redeploying a jar to change `nodes` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/composite/score/nodes = base:1,risk:2
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("score=" + score(p, Map.of("base", 10, "risk", 3, "loyalty", 5)));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("composite").folderOrCreate("score");
        if (!f.hasKey("nodes")) f.set("nodes", "base:1,risk:2,loyalty:1");
        if (!f.hasKey("enabled")) f.set("enabled", "base,risk,loyalty");
        return f;
    }
    static double score(Folder policy, Map<String, Integer> values) {
        Set<String> enabled = new HashSet<>();
        for (String e : read(policy, "enabled", "").split(",")) {
            if (!e.trim().isEmpty()) enabled.add(e.trim().toLowerCase());
        }
        double sum = 0, wsum = 0;
        for (String part : read(policy, "nodes", "").split(",")) {
            String[] kv = part.trim().split(":");
            if (kv.length != 2) continue;
            String id = kv[0].trim().toLowerCase();
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-composite-live-rules
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules)*
