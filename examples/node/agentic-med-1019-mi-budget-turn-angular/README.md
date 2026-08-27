# agentic-med-1019-mi-budget-turn

I Restarted Grok Build to Flip Token budget mid-session — The Admin Dashboard Wall Already Knew

**Product scene:** admin dashboard  
**Agent host:** Grok Build  
**Pain:** Token budget required killing the agent session

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-med-1019-mi-budget-turn/max-tokens = 8000
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-med-1019-mi-budget-turn` | `./gradlew test run` |
| Python | `examples/python/agentic-med-1019-mi-budget-turn` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-med-1019-mi-budget-turn-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-med-1019-mi-budget-turn-angular` | `npm install && npm test && node peer.mjs` |

React / Angular **server** entries (`createFromEnv`) hold Connect tokens.
The SPA talks to **your BFF** (`node peer.mjs --serve` → `GET /posture`).
Never put `KIPONOS_ID` / `KIPONOS_ACCESS` in the browser.

## Connect

Copy tokens from kiponos.io → **Connect**:

```bash
export KIPONOS_ID=…          # not committed
export KIPONOS_ACCESS=…
export KIPONOS="['my-app']['v1.0.0']['dev']['base']"
```

Java also accepts `kiponos.local.env` next to `build.gradle` (gitignored).

## This peer (angular)

```bash
cd examples/node/agentic-med-1019-mi-budget-turn-angular
npm install
npm test
node peer.mjs
KIPONOS_LIVE=1 node peer.mjs
node peer.mjs --serve   # BFF; SPA fetches /posture
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
