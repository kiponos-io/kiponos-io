# agentic-dev-1029-am-session-posture

MCP host Finished the Turn Blind — Session posture shared across hosts on the Shopping Path

**Product scene:** shopping  
**Agent host:** MCP host  
**Pain:** Second agent host needed a paste buffer to see the same war-room posture

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-dev-1029-am-session-posture/session-posture = focus=admin-wall,shopping-pause=off
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-dev-1029-am-session-posture` | `./gradlew test run` |
| Python | `examples/python/agentic-dev-1029-am-session-posture` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-dev-1029-am-session-posture-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-dev-1029-am-session-posture-angular` | `npm install && npm test && node peer.mjs` |

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

## This peer (java)

```bash
cd examples/java/agentic-dev-1029-am-session-posture
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
