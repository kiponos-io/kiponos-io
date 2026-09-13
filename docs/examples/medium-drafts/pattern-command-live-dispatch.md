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

That is the GoF-versus-live cut for `refund.enabled`.

<!-- medium-img: diagram-pattern-command-live-dispatch-gof-vs-live.png -->

That is the hub-flow cut: one leaf, every peer.

<!-- medium-img: diagram-pattern-command-live-dispatch-hub-flow.png -->
---

## Why this still matters on a quiet Tuesday

Incidents train the muscle. Quiet days keep it honest.

If `value` only moves through a release train, every product conversation becomes a ceremony debate: who owns the PR, who merges, who rolls, who watches. That tax compounds across regions and on-call rotations.

Live posture is not a license for chaos. It is a contract:

- named path humans can find under pressure  
- clamps that survive panic  
- audit that names the actor  
- a one-line revert that does not require a hero  

When those four exist, the Super Pattern stops being a demo and becomes how the system grows older without growing brittle.
<!-- kiponos-expanded: editor-words -->

## War-room addendum — pattern command live dispatch

I have sat in a 00:47 bridge where `refund.enabled` was already decided in a sentence and the process still served last week's jar. Someone offered the usual escape: we will cut a PR. A PR. While the room is already paying for the old number.

That is not architecture. That is a delayed email with extra ceremony.

Quiet Tuesdays are worse than incidents. Incidents at least force a decision. On a quiet day the tax hides in four services, three regions, and a wiki nobody trusts. Live posture is the contract: a named path, clamps, audit, one-line revert.

I have always believed people should not have to ship a release to make a decision the business already made out loud. With Kiponos on the same tree as the peers that must obey it, the session stays. The leaf moves.


## The example

Clone the golden tree and run it. The article is the nerve; the repo is the product.


## Operational checklist

1. Name the hub path so humans find `refund.enabled` under pressure.
2. Default safely when the hub is unreachable.
3. Allowlist writers (dashboard roles + automation identities).
4. Log the **decision**, not every get.
5. Rehearse the flip in staging.
6. Document the one-line revert.
