# Composite Super Pattern — Node Weights Live

*A traveler’s note: composite scoring weights live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

composite scoring weights live

Redeploying a jar to change `nodes` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/composite/score/nodes = base:1,risk:2
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("score=" + score(p, Map.of("base", 10, "risk", 3, "loyalty", 5)));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("composite").folderOrCreate("score");
        if (!f.hasKey("nodes")) f.set("nodes", "base:1,risk:2,loyalty:1");
        if (!f.hasKey("enabled")) f.set("enabled", "base,risk,loyalty");
        return f;
    }
    static double score(Folder policy, Map<String, Integer> values) {
        Set<String> enabled = new HashSet<>();
        for (String e : read(policy, "enabled", "").split(",")) {
            if (!e.trim().isEmpty()) enabled.add(e.trim().toLowerCase());
        }
        double sum = 0, wsum = 0;
        for (String part : read(policy, "nodes", "").split(",")) {
            String[] kv = part.trim().split(":");
            if (kv.length != 2) continue;
            String id = kv[0].trim().toLowerCase();
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-composite-live-rules
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-composite-live-rules)*

That is the GoF-versus-live cut for `nodes`.

<!-- medium-img: diagram-pattern-composite-live-rules-gof-vs-live.png -->

That is the hub-flow cut: one leaf, every peer.

<!-- medium-img: diagram-pattern-composite-live-rules-hub-flow.png -->
---

## Why this still matters on a quiet Tuesday

Incidents train the muscle. Quiet days keep it honest.

If `nodes` only moves through a release train, every product conversation becomes a ceremony debate: who owns the PR, who merges, who rolls, who watches. That tax compounds across regions and on-call rotations.

Live posture is not a license for chaos. It is a contract:

- named path humans can find under pressure  
- clamps that survive panic  
- audit that names the actor  
- a one-line revert that does not require a hero  

When those four exist, the Super Pattern stops being a demo and becomes how the system grows older without growing brittle.
<!-- kiponos-expanded: editor-words -->

## War-room addendum — pattern composite live rules

I have sat in a 03:25 bridge where `nodes` was already decided in a sentence and the process still served last week's jar. Someone offered the usual escape: we will cut a PR. A PR. While the room is already paying for the old number.

That is not architecture. That is a delayed email with extra ceremony.

Quiet Tuesdays are worse than incidents. Incidents at least force a decision. On a quiet day the tax hides in four services, three regions, and a wiki nobody trusts. Live posture is the contract: a named path, clamps, audit, one-line revert.

I have always believed people should not have to ship a release to make a decision the business already made out loud. With Kiponos on the same tree as the peers that must obey it, the session stays. The leaf moves.


## The example

Clone the golden tree and run it. The article is the nerve; the repo is the product.


## Operational checklist

1. Name the hub path so humans find `nodes` under pressure.
2. Default safely when the hub is unreachable.
3. Allowlist writers (dashboard roles + automation identities).
4. Log the **decision**, not every get.
5. Rehearse the flip in staging.
6. Document the one-line revert.
