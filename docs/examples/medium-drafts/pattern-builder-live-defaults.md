# Builder Super Pattern — Defaults and Severity Live

*A traveler’s note: builder defaults from hub.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

builder defaults from hub

Redeploying a jar to change `page-size` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/builder/report/page-size = 50
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Report r = build(p, args.length > 0 ? args[0] : "Q1");
            System.out.println(r);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("builder").folderOrCreate("report");
        if (!f.hasKey("page-size")) f.set("page-size", "50");
        if (!f.hasKey("include-charts")) f.set("include-charts", "yes");
        if (!f.hasKey("severity")) f.set("severity", "warn");
        return f;
    }
    static Report build(Folder policy, String title) {
        int page = readInt(policy, "page-size", 50);
        boolean charts = truthy(read(policy, "include-charts", "yes"));
        String severity = read(policy, "severity", "warn");
        if (page < 1) {
            if ("error".equalsIgnoreCase(severity)) throw new IllegalArgumentException("page-size");
            page = 50;
        }
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-builder-live-defaults
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-builder-live-defaults](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-builder-live-defaults)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-builder-live-defaults](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-builder-live-defaults)*
