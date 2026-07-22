# Live Ops Knob 2: Tune Without Redeploy

*A traveler’s note: Generic live knob knob-2 for continuous stream demo 2.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

Generic live knob knob-2 for continuous stream demo 2

Redeploying a jar to change `knob-2` is how teams invent 3am folklore.

---

## Hub tree

```text
examples/aha-generated-knob-002/knob-2 = 12
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("knob-2=" + read(p, "knob-2", "12"));
            System.out.println("Generic live knob knob-2 for continuous stream demo 2");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-generated-knob-002");
        if (!f.hasKey("knob-2")) {
            f.set("knob-2", "12");
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
cd kiponos-io/examples/java/aha-generated-knob-002
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-002](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-002)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-002](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/aha-generated-knob-002)*
