# agentic-mcp-0923-pm-lazy-schema

GitHub MCP Alone Was 26k Tokens. I Did Not Need All 35 Tools for This Turn

**Product scene:** travel  
**Agent host:** Grok Build  
**Pain:** MCP schemas ate the window before the user spoke

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-mcp-0923-pm-lazy-schema/schema-budget = 4000
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-mcp-0923-pm-lazy-schema` | `./gradlew test run` |
| Python | `examples/python/agentic-mcp-0923-pm-lazy-schema` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-mcp-0923-pm-lazy-schema-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-mcp-0923-pm-lazy-schema-angular` | `npm install && npm test && node peer.mjs` |

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
cd examples/java/agentic-mcp-0923-pm-lazy-schema
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
