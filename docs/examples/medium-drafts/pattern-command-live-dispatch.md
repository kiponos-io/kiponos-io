# Command Super Pattern — Dry-Run and Enable Live

*A traveler’s note: block or dry-run commands live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

block or dry-run commands live

Redeploying a jar to change `refund.enabled` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/command/dispatch/refund.enabled = yes
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "refund";
        long amount = args.length > 1 ? Long.parseLong(args[1]) : 500L;
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(dispatch(p, cmd, amount));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("command").folderOrCreate("dispatch");
        if (!f.hasKey("refund.enabled")) f.set("refund.enabled", "yes");
        if (!f.hasKey("refund.dry-run")) f.set("refund.dry-run", "no");
        return f;
    }
    static String dispatch(Folder policy, String cmd, long amount) {
        String c = cmd.toLowerCase(Locale.ROOT);
        if (!truthy(read(policy, c + ".enabled", "yes"))) return "blocked: disabled " + c;
        boolean dry = truthy(read(policy, c + ".dry-run", "no"));
        return (dry ? "DRY-RUN " : "EXEC ") + c + " amount=" + amount;
    }
    static boolean truthy(String s) {
        return s != null && (s.equalsIgnoreCase("yes")||s.
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-command-live-dispatch
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-command-live-dispatch](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-command-live-dispatch)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-command-live-dispatch](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-command-live-dispatch)*
