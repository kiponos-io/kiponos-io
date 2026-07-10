# Design: Kiponos Agent Kit (SDK 0.2 + MCP + Skill)

## Problem

Agents need **live** shared state with humans (dashboard) without restarts.  
A **Skill** alone documents usage but dies with the session. A raw **SDK** is correct but every agent re-implements connection and onboarding.

## Solution (three layers)

```
┌─────────────────────────────────────────────┐
│  Agent (Grok / Cursor / Claude / …)         │
│    Skill: when/how + onboarding script      │
└─────────────────┬───────────────────────────┘
                  │ MCP tools
┌─────────────────▼───────────────────────────┐
│  kiponos-mcp  (long-lived stdio process)    │
│    one Kiponos client, event buffer         │
└─────────────────┬───────────────────────────┘
                  │ Python API
┌─────────────────▼───────────────────────────┐
│  kiponos  SDK 0.2                           │
│    WebSocket/STOMP ↔ kiponos.io             │
└─────────────────────────────────────────────┘
                  │
            Dashboard (React, same tree)
```

## SDK 0.2 public API (clean)

| Method | Purpose |
|--------|---------|
| `Kiponos.connect(profile=…)` | Open + bootstrap |
| `get(path)` / `set(path, value)` | Nested slash paths |
| `list_keys` / `list_folders` / `dump` | Explore |
| `mkdir` / `ensure_path` | Folders |
| `delete_key` | Remove key |
| `on_change(cb)` | Live deltas (worker off listener thread) |
| `close()` / context manager | Lifecycle |

Auth: `KIPONOS_ID`, `KIPONOS_ACCESS`, profile `KIPONOS`.  
Wire: same as Java ReadyMode (`sdk-save-config-prop`, team topics, …).

**Not** the same package as skill-local `agent_client.py` experiments — those informed this design; 0.2 is the production surface.

## MCP integration

- **Why MCP:** process lifetime ≠ agent turn; tools are discoverable; env holds secrets once.
- **Tools:** status, CRUD paths, onboarding, poll_events.
- **No systemd** for “SDK service” — MCP replaces that role.

## Skill integration

- Triggers on Kiponos / live config / tokens language.
- Prefers MCP tools; CLI fallback.
- Encodes onboarding so any agent can walk a new user.

## Distribution

Free bundle under `kiponos-io/agent-kit/`:

- `pip install -e .` → `kiponos`, `kiponos-mcp`, `kiponos-cli`
- `./install.sh` → skill dirs + pip + MCP snippet

## Security

- Tokens only in env / MCP config  
- Skill forbids printing JWEs  
- Profile must match token connection  

## Future

- Promote event push (MCP notifications) when hosts support it  
- Parity matrix tests vs Java SDK  
- Control-plane key conventions as optional skill extension  
