# Flyweight Super Pattern — Cache Cap Live

*A traveler’s note: flyweight cache size live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

flyweight cache size live

Redeploying a jar to change `max-entries` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/flyweight/glyphs/max-entries = 3
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Map<String, String> cache = new LinkedHashMap<>();
            int max = readInt(p, "max-entries", 3);
            for (String g : List.of("A", "B", "C", "D")) {
                put(cache, max, g, "glyph-" + g);
            }
            System.out.println("cache=" + cache.keySet() + " max=" + max);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("flyweight").folderOrCreate("glyphs");
        if (!f.hasKey("max-entries")) f.set("max-entries", "3");
        return f;
    }
    static void put(Map<String, String> cache, int max, String k, String v) {
        cache.put(k, v);
        while (cache.size() > max) {
            String first = cache.keySet().iterator().next();
            cache.remove(first);
        }
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-flyweight-live-cache
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-flyweight-live-cache](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-flyweight-live-cache)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-flyweight-live-cache](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-flyweight-live-cache)*
