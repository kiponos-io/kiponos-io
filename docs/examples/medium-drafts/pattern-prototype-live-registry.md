# Prototype Super Pattern — Active Template Live

*A traveler’s note: which prototype to clone live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

which prototype to clone live

Redeploying a jar to change `active-template` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/prototype/docs/active-template = invoice-v1
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(cloneTemplate(p));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("prototype").folderOrCreate("docs");
        if (!f.hasKey("active-template")) f.set("active-template", "invoice-v1");
        return f;
    }
    static String cloneTemplate(Folder policy) {
        String id = read(policy, "active-template", "invoice-v1");
        return REGISTRY.getOrDefault(id, REGISTRY.get("invoice-v1")) + " [clone of " + id + "]";
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k); return r == null || r.isBlank() ? d : r.trim();
    }
    private PrototypeLiveRegistryApp() {}
}
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-prototype-live-registry
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-prototype-live-registry](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-prototype-live-registry)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-prototype-live-registry](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-prototype-live-registry)*
