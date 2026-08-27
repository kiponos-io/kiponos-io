# agentic-dev-0913-am-admin-wall

Cursor Finished the Turn Blind — Admin wall tile without SPA tokens on the Senses Path

**Product scene:** senses  
**Agent host:** Cursor  
**Pain:** Admin dashboard tile focus required a SPA rebuild

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-dev-0913-am-admin-wall/wall-focus = checkout
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-dev-0913-am-admin-wall` | `./gradlew test run` |
| Python | `examples/python/agentic-dev-0913-am-admin-wall` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-dev-0913-am-admin-wall-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-dev-0913-am-admin-wall-angular` | `npm install && npm test && node peer.mjs` |

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

## This peer (react)

```bash
cd examples/node/agentic-dev-0913-am-admin-wall-react
npm install
npm test
node peer.mjs
KIPONOS_LIVE=1 node peer.mjs
node peer.mjs --serve   # BFF; SPA fetches /posture
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
