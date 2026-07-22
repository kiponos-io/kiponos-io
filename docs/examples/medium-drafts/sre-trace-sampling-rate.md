# Trace Sampling Rate Live

*A traveler’s note: trace sample rate live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

trace sample rate live

Redeploying a jar to change `sample-rate` is how teams invent 3am folklore.

---

## Hub tree

```text
examples/sre-trace-sampling-rate/sample-rate = 0.1
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("sample-rate=" + read(p, "sample-rate", "0.1"));
            System.out.println("trace sample rate live");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("sre-trace-sampling-rate");
        if (!f.hasKey("sample-rate")) {
            f.set("sample-rate", "0.1");
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
cd kiponos-io/examples/java/sre-trace-sampling-rate
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-trace-sampling-rate](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-trace-sampling-rate)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-trace-sampling-rate](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/sre-trace-sampling-rate)*
