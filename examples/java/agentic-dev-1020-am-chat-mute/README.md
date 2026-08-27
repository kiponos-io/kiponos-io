# agentic-dev-1020-am-chat-mute

Claude Code Finished the Turn Blind — Group-chat mute without host kill on the Travel Path

**Product scene:** travel  
**Agent host:** Claude Code  
**Pain:** Killing the agent host to stop a flooded travel group chat

Hub leaf (same Team tree for every SDK):

```text
examples/agentic-dev-1020-am-chat-mute/chat-mute = none
```

Hot path: **local `get()`** after WebSocket bootstrap. Flip the leaf on the
[Kiponos.io](https://kiponos.io) dashboard — Grok Build / Cursor / Claude Code
do **not** restart MCP to honor it.

## Four peers (one leaf)

| SDK | Path | How to run |
|-----|------|------------|
| Java | `examples/java/agentic-dev-1020-am-chat-mute` | `./gradlew test run` |
| Python | `examples/python/agentic-dev-1020-am-chat-mute` | `python3 -m pytest -q && python3 peer.py` |
| React (Node BFF) | `examples/node/agentic-dev-1020-am-chat-mute-react` | `npm install && npm test && node peer.mjs` |
| Angular (Node BFF) | `examples/node/agentic-dev-1020-am-chat-mute-angular` | `npm install && npm test && node peer.mjs` |

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
cd examples/java/agentic-dev-1020-am-chat-mute
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Repo: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
