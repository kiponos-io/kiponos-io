---
title: "RAG Had CHUNK_SIZE=512 and TOP_K=8 in constants.py — Retrieval Storm Needed Different Knobs (Python + Kiponos)"
published: false
tags: python, ai, rag, devops
description: Chunk size, top-k, and rerank weights feel like offline tuning constants. During retrieval storms they are operational — Kiponos feeds live RAG policy with zero-latency reads on every query.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-rag-chunk-topk.md
main_image: https://files.catbox.moe/n1iaiq.jpg
---

Tuesday 2:14 PM. Legal drops a 400-page policy addendum into the knowledge base. Your support RAG stack ingests it overnight — but the retrieval service still runs with `CHUNK_SIZE = 512`, `TOP_K = 8`, and `RERANK_WEIGHT = 0.35` from `constants.py`, values chosen during a calm pilot when documents averaged twelve pages.

By 9:00 AM Wednesday, customer queries about refund eligibility return fragments from unrelated warranty clauses. Support agents paste hallucination-prone answers into Zendesk. The ML lead pings Slack:

> "We need **larger chunks** and **top_k=15** for policy week. Do not redeploy retrieval while ingest is still running."

But those numbers live in a module imported at worker boot. Changing them means recycling twelve Celery workers mid-ingest, losing warm vector index handles, and replaying a backlog of 40,000 embedding jobs. The ingest pipeline is healthy. **Retrieval policy** is wrong for the document shape landing *right now*.

Here is the Aha that lands for teams who have shipped RAG to production:

**Chunk size and top-k behave like sacred ML constants, but they are operational retrieval policy for this corpus hour.**

You can change `chunk_tokens`, `top_k`, and `rerank_semantic_weight` **while Python workers keep serving `/retrieve`** — no redeploy, no worker restart, no config poll per query. The next `search()` call already reads the new integers from memory. That is [Kiponos.io](https://kiponos.io).

## The problem — frozen RAG knobs on the query hot path

The pattern is everywhere in Python RAG services:

```python
# constants.py — imported once at worker boot
CHUNK_SIZE = 512
CHUNK_OVERLAP = 64
TOP_K = 8
RERANK_SEMANTIC_WEIGHT = 0.35
RERANK_KEYWORD_WEIGHT = 0.65
```

Your retrieval hot path inherits those decisions on every customer query:

```python
def search(query: str, vector_store, reranker) -> list[Document]:
    candidates = vector_store.similarity_search(
        query,
        k=TOP_K,
        chunk_size_hint=CHUNK_SIZE,
    )
    return reranker.rerank(
        query,
        candidates,
        semantic_weight=RERANK_SEMANTIC_WEIGHT,
        keyword_weight=RERANK_KEYWORD_WEIGHT,
    )
```

Legal's 400-page PDF changes the optimal chunk boundary. Short chunks fragment cross-references; `top_k=8` misses sections buried on page 287. ML knows the fix. Ops cannot reach into running workers. The pain is not ignorance — teams **know** retrieval quality is parameter-sensitive. They do not know there is a clean way to retune **without recycling the worker pool**.

| What teams believe | What production does |
|------------------|---------------------|
| "Chunk size is an offline indexing decision" | New corpus shapes demand different boundaries live |
| "We'll tune top_k in the next sprint" | Policy week ends Friday; the PR merges Monday |
| "Rerank weights are model-training artifacts" | Weights are **support SLA policy** for this incident |
| "Constants.py is fine for v1" | v1 runs during the worst document surge of the quarter |

## The Aha — live retrieval policy while workers run

Move chunk, top-k, and rerank knobs into Kiponos. Workers still boot from minimal env wiring — but **live RAG policy** lives in the hub:

```yaml
retrieval/
  chunking/
    chunk_tokens: 512
    chunk_overlap: 64
    max_chunk_tokens: 1024
  search/
    top_k: 8
    min_score: 0.72
    hybrid_enabled: true
  rerank/
    semantic_weight: 0.35
    keyword_weight: 0.65
    max_candidates: 40
  storm/
    policy_week_mode: false
    policy_week_top_k: 15
    policy_week_chunk_tokens: 768
```

During the legal addendum surge, ML enables `storm/policy_week_mode` and sets `policy_week_top_k: 15`. WebSocket delivers a **delta** — only those keys patch into the SDK's in-memory tree. Your `search()` reads the new integers on the **next** query — local `get_int()`, zero network. Workers unchanged. When the surge ends, disable `policy_week_mode` without redeploy.

## What is Kiponos.io — for RAG retrieval freshness

Kiponos is a real-time configuration hub. Your Python SDK connects once at worker startup, loads a typed tree for a profile path like `['support-rag']['prod']['retrieval']`, and holds the latest values **in process memory**. Dashboard edits arrive as WebSocket **deltas** — not a 60 KB YAML redeploy. Your request handler calls `kiponos.path("retrieval", "search").get_int("top_k")` and gets a **local read** in microseconds. No HTTP round trip. No Redis poll on every query.

That matters on the retrieval hot path: hundreds of concurrent support lookups, each potentially touching chunk hints, top-k, and rerank weights. You cannot afford remote config fetches per query. Kiponos separates **wiring** (team id, access key, profile path in env vars) from **operational floats** (chunk tokens, top_k, rerank weights) that ML and ops need to move during corpus events.

`after_value_changed` lets you react when policy flips hard — for example, clearing an in-process candidate cache when `top_k` jumps so stale shortlists do not linger across the transition.

## Architecture — how retrieval policy flows without redeploy

![Architecture diagram](https://files.catbox.moe/zo4t40.png)

1. **Connect once** at worker boot — `Kiponos.create_for_current_team()`.
2. **Full tree snapshot** loads for profile `['support-rag']['prod']['retrieval']`.
3. **Dashboard edit** sends **delta only** — not the entire retrieval policy file.
4. **SDK merges async** on a WebSocket worker thread.
5. **Reads are local** — your query handler never waits on the network.

This is why the Aha lands hard: the mental model flips from "constants.py + restart culture" to **"retrieval policy my process already holds."**

## Config tree — operational RAG folders

```yaml
retrieval/
  chunking/
    chunk_tokens: 512
    chunk_overlap: 64
    max_chunk_tokens: 1024
    split_on_headers: true
  search/
    top_k: 8
    min_score: 0.72
    hybrid_enabled: true
    keyword_boost: 1.2
  rerank/
    semantic_weight: 0.35
    keyword_weight: 0.65
    max_candidates: 40
    model_timeout_ms: 800
  storm/
    policy_week_mode: false
    policy_week_top_k: 15
    policy_week_chunk_tokens: 768
    invalidate_cache_on_toggle: true
```

## Integration — Kiponos-backed retrieval on the hot path

```python
import logging
import os
from dataclasses import dataclass

from kiponos import Kiponos

log = logging.getLogger(__name__)

os.environ.setdefault("KIPONOS_PROFILE", "['support-rag']['prod']['retrieval']")
kiponos = Kiponos.create_for_current_team()

_candidate_cache: dict[str, list] = {}


@dataclass(frozen=True)
class RetrievalPolicy:
    chunk_tokens: int
    top_k: int
    min_score: float
    semantic_weight: float
    keyword_weight: float
    max_candidates: int


def _load_policy() -> RetrievalPolicy:
    storm = kiponos.path("retrieval", "storm")
    if storm.get_bool("policy_week_mode", False):
        return RetrievalPolicy(
            chunk_tokens=storm.get_int("policy_week_chunk_tokens", 768),
            top_k=storm.get_int("policy_week_top_k", 15),
            min_score=kiponos.path("retrieval", "search").get_float("min_score", 0.72),
            semantic_weight=kiponos.path("retrieval", "rerank").get_float("semantic_weight", 0.35),
            keyword_weight=kiponos.path("retrieval", "rerank").get_float("keyword_weight", 0.65),
            max_candidates=kiponos.path("retrieval", "rerank").get_int("max_candidates", 40),
        )
    chunk = kiponos.path("retrieval", "chunking")
    search = kiponos.path("retrieval", "search")
    rerank = kiponos.path("retrieval", "rerank")
    return RetrievalPolicy(
        chunk_tokens=chunk.get_int("chunk_tokens", 512),
        top_k=search.get_int("top_k", 8),
        min_score=search.get_float("min_score", 0.72),
        semantic_weight=rerank.get_float("semantic_weight", 0.35),
        keyword_weight=rerank.get_float("keyword_weight", 0.65),
        max_candidates=rerank.get_int("max_candidates", 40),
    )


def _on_policy_change(change) -> None:
    if not str(change.path).startswith("retrieval/"):
        return
    storm = kiponos.path("retrieval", "storm")
    if storm.get_bool("invalidate_cache_on_toggle", False):
        _candidate_cache.clear()
        log.info("Cleared retrieval candidate cache after policy change: %s", change.path)


kiponos.after_value_changed(_on_policy_change)


def search(query: str, vector_store, reranker) -> list:
    policy = _load_policy()
    cache_key = f"{query}:{policy.top_k}:{policy.chunk_tokens}"
    if cache_key in _candidate_cache:
        candidates = _candidate_cache[cache_key]
    else:
        candidates = vector_store.hybrid_search(
            query,
            k=policy.top_k,
            chunk_tokens=policy.chunk_tokens,
            min_score=policy.min_score,
            keyword_boost=kiponos.path("retrieval", "search").get_float("keyword_boost", 1.2),
        )
        _candidate_cache[cache_key] = candidates

    trimmed = candidates[: policy.max_candidates]
    return reranker.rerank(
        query,
        trimmed,
        semantic_weight=policy.semantic_weight,
        keyword_weight=policy.keyword_weight,
        timeout_ms=kiponos.path("retrieval", "rerank").get_int("model_timeout_ms", 800),
    )
```

Policy week starts? ML enables `policy_week_mode`. **Next `search()`** uses fifteen candidates and 768-token chunk hints. Major toggle with `invalidate_cache_on_toggle: true`? Shortlist cache clears without worker restart.

## Real scenarios — emotional → operational

| Moment | Hard-coded constants reflex | Kiponos path |
|--------|------------------------------|--------------|
| 400-page legal PDF lands | Redeploy workers or accept fragmented answers | `storm/policy_week_mode: true` live |
| Retrieval latency spike | Emergency branch per environment | Drop `top_k` to 5 from dashboard |
| Cross-encoder reranker slow | Static `max_candidates=40` overloads GPU | Lower `max_candidates` instantly |
| Post-surge steady state | Leave aggressive top_k burning rerank budget | Disable `policy_week_mode` |
| A/B test chunk strategy | Two deploy tracks | Hub profile `support-rag/chunk-experiment` |

Pair with [embedding cache TTL tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-ai-embedding-cache-ttl.md) — re-index events and retrieval storms often arrive together.

## Compare to alternatives

| Approach | Mid-surge top_k change | Read cost on hot path |
|----------|------------------------|------------------------|
| `constants.py` module constants | PR + worker recycle (20+ min) | Zero (frozen) |
| Env var at Celery boot | Rolling worker restart | Zero after restart |
| Poll Redis for RAG policy | Dashboard-fast | Network RTT per query |
| Feature flag for boolean only | Fast toggle | Cannot tune floats |
| **Kiponos SDK** | **Dashboard delta (seconds)** | **Memory read** |

## Performance — why RAG teams care

- One WebSocket per worker lifetime — not one config fetch per query
- `_load_policy()` composes O(1) `get_int()` / `get_float()` reads — safe inside tight search loops
- `_candidate_cache` invalidates on `after_value_changed`, not per request
- Vector store still owns indexing; Kiponos only feeds the operational integers
- Storm mode resolution happens once per `search()` — no nested remote calls

## When not to use Kiponos for RAG policy

| Case | Better approach |
|------|-----------------|
| Embedding model version (`text-embedding-3-large` → `small`) | Versioned index + controlled re-embed job |
| Vector database shard topology | GitOps / infra-as-code |
| Chunking tokenizer or splitter algorithm swap | Code review + deployment |
| API keys for OpenAI / Cohere rerankers | Vault |
| Replacing FAISS with Pinecone | Architecture migration |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — create profile `['support-rag']['prod']['retrieval']`.
2. Move **five** keys out of `constants.py`: `chunk_tokens`, `top_k`, `semantic_weight`, `keyword_weight`, `min_score`.
3. Replace module-level constants with `_load_policy()` that reads from Kiponos.
4. Add `after_value_changed` handler for optional candidate cache invalidation.
5. Rehearsal: simulate policy week in staging, enable `policy_week_mode`, verify broader retrieval **without worker restart**.
6. Document boundary: Git declares retrieval wiring; hub declares **operational corpus policy**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — chunk size and top_k are how much context you fetch right now, not constants.py forever.*