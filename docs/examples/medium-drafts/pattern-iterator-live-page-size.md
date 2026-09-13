# Iterator Super Pattern — Page Size Live

*A traveler’s note: pagination page size live.*

---

There is a class of production decisions that are **too small for a release** and **too important for a wiki**.

pagination page size live

Redeploying a jar to change `page-size` is how teams invent 3am folklore.

---

## Hub tree

```text
patterns/iterator/catalog/page-size = 25
```

Local `get()` on the hot path. Dashboard or remote SDK `set()` when the world changes.

---

## Snippet

```java
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            int page = readInt(p, "page-size", 25);
            List<String> items = List.of("a","b","c","d","e","f","g");
            System.out.println("page0=" + pageOf(items, page, 0));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("iterator").folderOrCreate("catalog");
        if (!f.hasKey("page-size")) f.set("page-size", "25");
        return f;
    }
    static List<String> pageOf(List<String> items, int pageSize, int pageIndex) {
        int from = Math.max(0, pageIndex * pageSize);
        if (from >= items.size()) return List.of();
        int to = Math.min(items.size(), from + pageSize);
        return items.subList(from, to);
    }
    static int readInt(Folder p, String k, int d) {
        try {
            if (!p.hasKey(k)) return d;
```

---

## Clone and run the full golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-iterator-live-page-size
cp kiponos.local.env.example kiponos.local.env   # tokens from kiponos.io → Connect
./gradlew test run
```

Full source + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size)

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

*Example + tests: [https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-iterator-live-page-size)*

That is the GoF-versus-live cut for `page-size`.

<!-- medium-img: diagram-pattern-iterator-live-page-size-gof-vs-live.png -->

That is the hub-flow cut: one leaf, every peer.

<!-- medium-img: diagram-pattern-iterator-live-page-size-hub-flow.png -->
---

## Why this still matters on a quiet Tuesday

Incidents train the muscle. Quiet days keep it honest.

If `page-size` only moves through a release train, every product conversation becomes a ceremony debate: who owns the PR, who merges, who rolls, who watches. That tax compounds across regions and on-call rotations.

Live posture is not a license for chaos. It is a contract:

- named path humans can find under pressure  
- clamps that survive panic  
- audit that names the actor  
- a one-line revert that does not require a hero  

When those four exist, the Super Pattern stops being a demo and becomes how the system grows older without growing brittle.
<!-- kiponos-expanded: editor-words -->

## War-room addendum — pattern iterator live page size

I have sat in a 04:51 bridge where `page-size` was already decided in a sentence and the process still served last week's jar. Someone offered the usual escape: we will cut a PR. A PR. While the room is already paying for the old number.

That is not architecture. That is a delayed email with extra ceremony.

Quiet Tuesdays are worse than incidents. Incidents at least force a decision. On a quiet day the tax hides in four services, three regions, and a wiki nobody trusts. Live posture is the contract: a named path, clamps, audit, one-line revert.

I have always believed people should not have to ship a release to make a decision the business already made out loud. With Kiponos on the same tree as the peers that must obey it, the session stays. The leaf moves.


## The example

Clone the golden tree and run it. The article is the nerve; the repo is the product.


## Operational checklist

1. Name the hub path so humans find `page-size` under pressure.
2. Default safely when the hub is unreachable.
3. Allowlist writers (dashboard roles + automation identities).
4. Log the **decision**, not every get.
5. Rehearse the flip in staging.
6. Document the one-line revert.
