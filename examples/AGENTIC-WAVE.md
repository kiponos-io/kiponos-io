# Agentic wave — four SDK examples per article

Each calendar article has a **full mesh** on the same Team leaf:

| SDK | Root | Token rule |
|-----|------|------------|
| Java | `examples/java/<id>` | `kiponos.local.env` / env |
| Python | `examples/python/<id>` | env + optional `agent-kit` |
| React | `examples/node/<id>-react` | **Node BFF** `@kiponos/react/server` |
| Angular | `examples/node/<id>-angular` | **Node BFF** `@kiponos/angular/server` |

SPA never holds Connect tokens. `node peer.mjs --serve` exposes `GET /posture`.

**Count:** 60 article ids × 4 SDKs.

## Ids

- `agentic-mcp-0908-am-schema-budget` — `schema-budget` (admin-dashboard, Grok Build)
- `agentic-mcp-0908-pm-tools-roster` — `tools-allow` (shopping, Cursor)
- `agentic-mcp-0909-am-context-rot` — `compact-mode` (senses, Cursor)
- `agentic-mcp-0909-pm-tool-space` — `tools-allow` (travel, Claude Code)
- `agentic-mcp-0910-am-result-cap` — `result-cap` (admin-dashboard, Claude Code)
- `agentic-mcp-0910-pm-progressive` — `tool-search` (mirror-phone, MCP host)
- `agentic-mcp-0911-am-zombie-drain` — `mcp-drain` (senses, MCP host)
- `agentic-mcp-0911-pm-write-gate` — `tools-allow` (shopping, Grok Build)
- `agentic-mcp-0912-am-skill-once` — `enabled-set` (admin-dashboard, Grok Build)
- `agentic-mcp-0912-pm-skill-vs-mcp` — `shared-truth` (travel, Cursor)
- `agentic-mcp-0913-am-skill-trust` — `skill-trust` (senses, Cursor)
- `agentic-mcp-0913-pm-llm-skill-budget` — `skill-budget` (mirror-phone, Claude Code)
- `agentic-mcp-0914-am-two-hosts` — `session-posture` (admin-dashboard, Claude Code)
- `agentic-mcp-0914-pm-subagent-inherit` — `inherit-posture` (shopping, MCP host)
- `agentic-mcp-0915-am-execute-gate` — `execute-gate` (travel, MCP host)
- `agentic-mcp-0915-pm-cancel-mcp` — `cancel-token` (senses, Grok Build)
- `agentic-mcp-0916-am-argv-frozen` — `tools-allow` (mirror-phone, Grok Build)
- `agentic-mcp-0916-pm-selection-drop` — `tools-allow` (admin-dashboard, Cursor)
- `agentic-mcp-0917-am-doom-loop` — `retry-max` (shopping, Cursor)
- `agentic-mcp-0917-pm-code-mode` — `code-mode` (travel, Claude Code)
- `agentic-mcp-0918-am-client-blame` — `tool-search` (senses, Claude Code)
- `agentic-mcp-0918-pm-oauth-not-knob` — `session-posture` (admin-dashboard, MCP host)
- `agentic-mcp-0919-am-flags-not-session` — `tools-allow` (shopping, MCP host)
- `agentic-mcp-0919-pm-vector-not-posture` — `shared-truth` (travel, Grok Build)
- `agentic-mcp-0920-am-primitive` — `shared-truth` (admin-dashboard, Grok Build)
- `agentic-mcp-0920-pm-canary-tools` — `canary-percent` (mirror-phone, Cursor)
- `agentic-mcp-0921-am-tools-mute` — `tools-mute` (senses, Cursor)
- `agentic-mcp-0921-pm-token-midturn` — `max-tokens` (shopping, Claude Code)
- `agentic-mcp-0922-am-incident-writes` — `incident-pause` (shopping, Claude Code)
- `agentic-mcp-0922-pm-sense-stale` — `priority` (senses, MCP host)
- `agentic-mcp-0923-am-war-room` — `session-posture` (admin-dashboard, MCP host)
- `agentic-mcp-0923-pm-lazy-schema` — `schema-budget` (travel, Grok Build)
- `agentic-mcp-0924-am-cursor-cap` — `tools-allow` (mirror-phone, Grok Build)
- `agentic-mcp-0924-pm-openai-128` — `tools-allow` (admin-dashboard, Cursor)
- `agentic-mcp-0925-am-block-linear` — `result-cap` (travel, Cursor)
- `agentic-mcp-0925-pm-perplexity-exit` — `shared-truth` (senses, Claude Code)
- `agentic-mcp-0926-am-tau-bench` — `tools-allow` (travel, Claude Code)
- `agentic-mcp-0926-pm-linear-65x` — `result-cap` (admin-dashboard, MCP host)
- `agentic-mcp-0927-am-install-this-week` — `tools-allow` (shopping, MCP host)
- `agentic-mcp-0927-pm-rules-vs-skills` — `enabled-set` (mirror-phone, Grok Build)
- `agentic-mcp-0928-am-parallel-stale` — `inherit-posture` (senses, Grok Build)
- `agentic-mcp-0928-pm-host-ceremony` — `tools-allow` (admin-dashboard, Cursor)
- `agentic-mcp-0929-am-stdio-lifetime` — `mcp-drain` (travel, Cursor)
- `agentic-mcp-0929-pm-http-mcp` — `tools-allow` (shopping, Claude Code)
- `agentic-mcp-0930-am-namespace-tools` — `tools-allow` (admin-dashboard, Claude Code)
- `agentic-mcp-0930-pm-plugin-vs-hub` — `enabled-set` (mirror-phone, MCP host)
- `agentic-mcp-1001-am-bloat-confusion` — `schema-budget` (senses, MCP host)
- `agentic-mcp-1001-pm-exact-count` — `result-cap` (travel, Grok Build)
- `agentic-mcp-1002-am-midturn-flip` — `tools-allow` (shopping, Grok Build)
- `agentic-mcp-1002-pm-embedded-native` — `shared-truth` (admin-dashboard, Cursor)
- `agentic-mcp-1003-am-grok-native-mcp` — `tools-allow` (senses, Cursor)
- `agentic-mcp-1003-pm-claude-tool-search` — `tool-search` (travel, Claude Code)
- `agentic-mcp-1004-am-marketplace-pollute` — `skill-trust` (mirror-phone, Claude Code)
- `agentic-mcp-1004-pm-streaming-noise` — `result-cap` (shopping, MCP host)
- `agentic-mcp-1005-am-owner-folder` — `owner-agent` (travel, MCP host)
- `agentic-mcp-1005-pm-sandbox-not-posture` — `execute-gate` (admin-dashboard, Grok Build)
- `agentic-mcp-1006-am-reload-r` — `tools-allow` (senses, Grok Build)
- `agentic-mcp-1006-pm-hyposcale-stateless` — `shared-truth` (travel, Cursor)
- `agentic-mcp-1007-am-unaware-primitive` — `shared-truth` (admin-dashboard, Cursor)
- `agentic-mcp-1007-pm-turn-clock` — `max-tokens` (shopping, Claude Code)
