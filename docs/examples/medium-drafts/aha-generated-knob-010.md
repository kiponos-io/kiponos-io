# Live Ops Knob 10: Tune Without Redeploy

*A traveler’s note: Generic live knob knob-10 for continuous stream demo 10.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

Generic live knob knob-10 for continuous stream demo 10

Redeploying a jar to change `knob-10` is how teams invent 3am folklore.

---

## Hub tree

```text
examples/aha-generated-knob-010/knob-10 = 20
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("knob-10=" + read(p, "knob-10", "20"));
            System.out.println("Generic live knob knob-10 for continuous stream demo 10");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-generated-knob-010");
        if (!f.hasKey("knob-10")) {
            f.set("knob-10", "20");
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
cd kiponos-io/examples/java/aha-generated-knob-010
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-010](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-010)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-010](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-010)*
