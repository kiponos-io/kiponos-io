# Interpreter Super Pattern — Fail-Closed Live Rules

*A traveler’s note: mini rules language from hub.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

mini rules language from hub

Redeploying a jar to change `rules` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/interpreter/access/rules = * => deny
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        String role = args.length > 0 ? args[0] : "admin";
        String country = args.length > 1 ? args[1] : "US";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(eval(p, Map.of("role", role, "country", country)));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("interpreter").folderOrCreate("access");
        if (!f.hasKey("rules")) f.set("rules", "role=admin => allow; country=US => allow; * => deny");
        return f;
    }
    static String eval(Folder policy, Map<String, String> ctx) {
        String rules = read(policy, "rules", "* => deny");
        for (String rule : rules.split(";")) {
            String r = rule.trim();
            if (r.isEmpty()) continue;
            String[] parts = r.split("=>");
            if (parts.length != 2) continue;
            String cond = parts[0].trim();
            String action = parts[1].trim().toLowerCase(Locale.ROOT);
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-interpreter-live-rules
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-interpreter-live-rules](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-interpreter-live-rules)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-interpreter-live-rules](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-interpreter-live-rules)*
