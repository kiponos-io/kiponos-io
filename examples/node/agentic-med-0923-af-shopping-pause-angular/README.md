# agentic-med-0923-af-shopping-pause

I Restarted MCP host to Flip Shopping incident pause as a leaf — The Senses Wall Already Knew

**Product scene:** senses  
**Agent host:** MCP host  
**Pain:** Shopping freeze waited for an MCP restart

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-med-0923-af-shopping-pause/incident-pause = off
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-med-0923-af-shopping-pause` | `./gradlew test run` |
| Python | `examples/python/agentic-med-0923-af-shopping-pause` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-med-0923-af-shopping-pause-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-med-0923-af-shopping-pause-angular` | `npm install && npm test && node peer.mjs` |

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
cd examples/node/agentic-med-0923-af-shopping-pause-angular
npm install
npm test
node peer.mjs
KIPONOS_LIVE=1 node peer.mjs
node peer.mjs --serve   # BFF; SPA fetches /posture
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
