# Observer Super Pattern — Live Subscriber Enable List

*A traveler’s note: enable/debounce observers live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

enable/debounce observers live

Redeploying a jar to change `enabled-subscribers` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/observer/bus/enabled-subscribers = metrics,audit
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        String event = args.length > 0 ? args[0] : "order.paid";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            List<String> trail = publish(p, event);
            System.out.println("event=" + event + " trail=" + trail);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("observer").folderOrCreate("bus");
        if (!f.hasKey("enabled-subscribers")) f.set("enabled-subscribers", "metrics,audit");
        if (!f.hasKey("debounce-ms")) f.set("debounce-ms", "100");
        return f;
    }
    static List<String> publish(Folder policy, String event) {
        List<String> enabled = csv(read(policy, "enabled-subscribers", "metrics,audit"));
        int debounce = readInt(policy, "debounce-ms", 100);
        List<String> trail = new ArrayList<>();
        trail.add("debounce=" + debounce + "ms");
        for (String s : enabled) {
            trail.add(s + ":onEvent(" + event + ")");
        }
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-observer-live-bus
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-observer-live-bus](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-observer-live-bus)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-observer-live-bus](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-observer-live-bus)*
