# agentic-mcp-0923-am-war-room

The War Room Had Four Agent Windows and Four Different MCP Rosters

**Product scene:** admin dashboard  
**Agent host:** MCP host  
**Pain:** Second agent host needed a paste buffer to see the same war-room posture

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-mcp-0923-am-war-room/session-posture = focus=admin-wall,shopping-pause=off
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-mcp-0923-am-war-room` | `./gradlew test run` |
| Python | `examples/python/agentic-mcp-0923-am-war-room` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-mcp-0923-am-war-room-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-mcp-0923-am-war-room-angular` | `npm install && npm test && node peer.mjs` |

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

## This peer (python)

```bash
cd examples/python/agentic-mcp-0923-am-war-room
python3 -m pytest -q
python3 peer.py
KIPONOS_LIVE=1 python3 peer.py   # optional live hub
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
