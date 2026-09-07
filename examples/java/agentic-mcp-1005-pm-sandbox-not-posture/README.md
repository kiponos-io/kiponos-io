# agentic-mcp-1005-pm-sandbox-not-posture

E2B Sandbox Executed the Code. The Policy to Run It Lived in a Sticky Note

**Product scene:** admin dashboard  
**Agent host:** Grok Build  
**Pain:** Leaving plan mode required a host restart to unlock MCP write

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-mcp-1005-pm-sandbox-not-posture/execute-gate = plan
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-mcp-1005-pm-sandbox-not-posture` | `./gradlew test run` |
| Python | `examples/python/agentic-mcp-1005-pm-sandbox-not-posture` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-mcp-1005-pm-sandbox-not-posture-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-mcp-1005-pm-sandbox-not-posture-angular` | `npm install && npm test && node peer.mjs` |

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
cd examples/java/agentic-mcp-1005-pm-sandbox-not-posture
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
