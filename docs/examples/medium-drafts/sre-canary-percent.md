# Canary Percent Live

*A traveler’s note: canary traffic percent live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

canary traffic percent live

Redeploying a jar to change `percent` is how teams invent 3am folklore.

---

## Hub tree

```text
examples/sre-canary-percent/percent = 5
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("percent=" + read(p, "percent", "5"));
            System.out.println("canary traffic percent live");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("sre-canary-percent");
        if (!f.hasKey("percent")) {
            f.set("percent", "5");
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
cd kiponos-io/examples/java/sre-canary-percent
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-canary-percent](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-canary-percent)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-canary-percent](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-canary-percent)*
