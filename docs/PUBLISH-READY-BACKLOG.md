# Publish-ready examples backlog (code + articles)

**Goal:** When a Medium/dev.to slot opens, ship from disk — never invent under deadline.

**Public repo:** https://github.com/kiponos-io/kiponos-io  
**Convention:** Article = traveler prose + **snippets** + link to **full golden example**.  
**Uniqueness:** Medium ← `docs/examples/medium-drafts/<id>.md` · dev.to ← `docs/devto-*.md` (never cross-post).

---

## Super Patterns — status

| ID | Java | Python | Medium draft | dev.to essay | Notes |
|----|------|--------|--------------|--------------|-------|
| pattern-strategy-live-router | ✅ | ✅ | ✅ | ✅ | shipped / ready |
| pattern-decorator-live-chain | ✅ | ✅ | ✅ | ✅ | shipped / ready |
| pattern-chain-live-fraud | ✅ | ✅ | ✅ | ✅ | **ready to publish** |
| pattern-state-live-order | ✅ | ✅ | ✅ | ✅ | **ready to publish** |
| pattern-factory-live-channel | ✅ | ✅ | ✅ | ✅ | **ready to publish** |
| pattern-adapter-live-psp | ✅ | ⏳ | ✅ draft | ⏳ | Wave B |
| pattern-proxy-live-access | ✅ | ⏳ | ✅ draft | ⏳ | Wave B |
| pattern-abstract-factory-live-region | ✅ | ⏳ | ✅ draft | ⏳ | Wave B |
| pattern-bridge-live-implementor | ✅ | ⏳ | ✅ draft | ⏳ | Wave B |
| pattern-facade-live-knobs | ✅ | ⏳ | ✅ draft | ⏳ | Wave B |
| Wave C–D (observer…visitor) | planned | planned | planned | planned | see `docs-queues/super-patterns-gof-backlog.md` |

### Reader path (every Super Pattern)

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/<id>
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Python: `examples/python/<id>/` (pytest-less unit functions + optional live hub).

---

## Waves 15–30 (aha / industry / compare / finops / …)

Master queues live under `~/work/kiponos-io-ops/docs-queues/`:

- `waves-15-30-master-queue.txt` — essay basenames for dual-outlet
- Per-wave files: `wave15-…` … `wave30-…`

### Preparation standard (for each wave item)

1. **Code example** under `examples/java/<id>/` (logic test always; golden optional skip without tokens)  
2. **Python parity** under `examples/python/<id>/` when the story is SDK-generic  
3. **Medium draft** `docs/examples/medium-drafts/<id>.md` with:
   - traveler lede  
   - hub tree  
   - one short snippet  
   - **clone/run block** pointing at GitHub  
   - moral  
4. **dev.to unique essay** `docs/devto-<slug>.md` (different prose)  
5. Media: cover + 1 diagram (can generate at publish week)  
6. Append to dual-outlet queue only when 1–4 are green  

### Wave themes (existing queue inventory)

| Wave | Theme | Queue file |
|------|-------|------------|
| 15 | AI / ML / LLM | wave15-ai-ml-publish-queue.txt |
| 16 | Industry round 2 | wave16-industry-publish-queue.txt |
| 17 | Aha infrastructure | wave17-aha-infra-publish-queue.txt |
| 18 | Kubernetes | wave18-k8s-publish-queue.txt |
| 19 | Microservices | wave19-microservices-publish-queue.txt |
| 20 | SRE / architecture | wave20-sre-arch-publish-queue.txt |
| 21–23 | Comparisons | wave21–23-compare-*.txt |
| 24 | FinOps | wave24-finops-publish-queue.txt |
| 25 | Reliability | wave25-reliability-publish-queue.txt |
| 26 | RegOps | wave26-regops-publish-queue.txt |
| 27 | DataOps | wave27-dataops-publish-queue.txt |
| 28 | EdgeOps | wave28-edgeops-publish-queue.txt |
| 29 | Buy guides | wave29-buy-guide-publish-queue.txt |
| 30 | Operational patterns | wave30-pattern-publish-queue.txt |

Many **dev.to aha essays already exist** under `docs/devto-aha-*.md` and industry titles.  
Gap to close while idle: **matching `examples/java/*` goldens** so every essay can say “clone and run.”

### Priority while idle (after Super Patterns)

1. Finish Super Pattern Wave C (observer, command, template, mediator, interpreter)  
2. For each aha essay missing an example: scaffold `examples/java/aha-<knob>/` with one live key + logic test  
3. Python only where the knob is language-agnostic  
4. Keep Medium drafts thick (≥500 words traveler voice) before organic slots  

---

## Scaffold helper

```bash
python3 scripts/scaffold_super_pattern_example.py pattern-observer-live-bus \
  --main-class io.kiponos.examples.patterns.observer.ObserverLiveBusApp
```

Then implement App + LogicTest + GoldenTest + README + medium draft.

---

## Definition of “ready to publish”

- [ ] `./gradlew test` passes LogicTest without tokens  
- [ ] README has clone/run + hub tree  
- [ ] Medium draft ≥ ~500 words, snippets only, GH link for full example  
- [ ] (optional) Python parity tests pass  
- [ ] Cover/diagram paths reserved under `docs/examples/media/<id>/`  
- [ ] Not cross-posted Medium ↔ dev.to  

Updated: 2026-07-22
