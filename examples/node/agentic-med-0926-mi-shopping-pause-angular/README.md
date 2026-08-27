# agentic-med-0926-mi-shopping-pause

I Restarted Cursor to Flip Shopping incident pause as a leaf — The Shopping Wall Already Knew

**Product scene:** shopping  
**Agent host:** Cursor  
**Pain:** Shopping freeze waited for an MCP restart

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-med-0926-mi-shopping-pause/incident-pause = off
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-med-0926-mi-shopping-pause` | `./gradlew test run` |
| Python | `examples/python/agentic-med-0926-mi-shopping-pause` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-med-0926-mi-shopping-pause-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-med-0926-mi-shopping-pause-angular` | `npm install && npm test && node peer.mjs` |

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
cd examples/node/agentic-med-0926-mi-shopping-pause-angular
npm install
npm test
node peer.mjs
KIPONOS_LIVE=1 node peer.mjs
node peer.mjs --serve   # BFF; SPA fetches /posture
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
