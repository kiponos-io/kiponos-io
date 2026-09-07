# agentic-mcp-0920-pm-canary-tools

I Wanted 10% of Turns to See the New MCP Server. I Got 100% or a Restart

**Product scene:** mirror phone  
**Agent host:** Cursor  
**Pain:** New MCP server was all-or-nothing per host process

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-mcp-0920-pm-canary-tools/canary-percent = 0
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-mcp-0920-pm-canary-tools` | `./gradlew test run` |
| Python | `examples/python/agentic-mcp-0920-pm-canary-tools` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-mcp-0920-pm-canary-tools-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-mcp-0920-pm-canary-tools-angular` | `npm install && npm test && node peer.mjs` |

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
cd examples/node/agentic-mcp-0920-pm-canary-tools-angular
npm install
npm test
node peer.mjs
KIPONOS_LIVE=1 node peer.mjs
node peer.mjs --serve   # BFF; SPA fetches /posture
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
