# Memento Super Pattern — Snapshot Retention Live

*A traveler’s note: undo depth live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

undo depth live

Redeploying a jar to change `max-snapshots` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/memento/editor/max-snapshots = 3
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Deque<String> snaps = new ArrayDeque<>();
            int max = readInt(p, "max-snapshots", 3);
            for (String s : List.of("v1", "v2", "v3", "v4")) push(snaps, max, s);
            System.out.println("snapshots=" + snaps + " max=" + max);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("memento").folderOrCreate("editor");
        if (!f.hasKey("max-snapshots")) f.set("max-snapshots", "3");
        return f;
    }
    static void push(Deque<String> snaps, int max, String s) {
        snaps.addLast(s);
        while (snaps.size() > max) snaps.removeFirst();
    }
    static int readInt(Folder p, String k, int d) {
        try {
            if (!p.hasKey(k)) return d;
            return Integer.parseInt(p.get(k).trim());
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-memento-live-retention
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-memento-live-retention](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-memento-live-retention)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-memento-live-retention](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-memento-live-retention)*
