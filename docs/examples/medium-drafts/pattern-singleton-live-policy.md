# Singleton Super Pattern — One Instance Live Policy

*A traveler’s note: singleton policy knobs live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

singleton policy knobs live

Redeploying a jar to change `mode` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/singleton/gate/mode = open
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("mode=" + get().mode(p));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("singleton").folderOrCreate("gate");
        if (!f.hasKey("mode")) f.set("mode", "open");
        return f;
    }
    String mode(Folder policy) {
        if (!policy.hasKey("mode")) return "open";
        String r = policy.get("mode");
        return r == null || r.isBlank() ? "open" : r.trim().toLowerCase();
    }
}
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-singleton-live-policy
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-singleton-live-policy](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-singleton-live-policy)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-singleton-live-policy](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-singleton-live-policy)*
