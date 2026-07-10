---
name: kiponos-live
description: >
  Real-time Kiponos.io config for agents via SDK + MCP: get/set nested keys,
  folders, live value-change events, onboarding to tokens/profile. Use when the
  user mentions Kiponos live config, dashboard state, agent control plane,
  KIPONOS_ID, /kiponos-live, or installing the Kiponos agent kit. Prefer MCP tools
  when the kiponos MCP server is connected; otherwise use the Python SDK CLI.
metadata:
  author: kiponos-io
  version: "0.2.0"
  homepage: https://kiponos.io
  bundle: kiponos-agent-kit
---

# Kiponos Live (Skill + MCP + SDK)

**Architecture**

| Layer | Role |
|-------|------|
| **MCP** `kiponos` | Long-lived process; tools; one WebSocket connection |
| **Skill** (this file) | How the agent onboards the user and when to call tools |
| **SDK** `kiponos` | Python client (also used by the MCP) |

Do **not** invent a systemd service for the SDK. The MCP **is** the long-lived hub.

## First session checklist

1. Is MCP `kiponos` connected? If tools missing → install kit + add MCP (see below).
2. Call **`kiponos_status`**. If fail → **`kiponos_onboarding`** and walk the user through tokens.
3. Never echo full `KIPONOS_ID` / `KIPONOS_ACCESS` values.
4. Tokens and profile path **must match** the same Connect screen entry.

## Onboarding (agent directs user)

1. Open **https://kiponos.io** → free account / login  
2. **Connect Your App** (or Configs) → create/select app · release · env · config  
3. Copy **KIPONOS_ID**, **KIPONOS_ACCESS**, and bracket **profile**  
   e.g. `['my-app']['v1.0.0']['dev']['base']`  
4. User exports env (or configures MCP `env`) and agent retries **`kiponos_status`**

Handshake **500** ≈ mismatched tokens/profile, not “missing internet”.

## MCP tools (preferred)

| Tool | Purpose |
|------|---------|
| `kiponos_onboarding` | Steps + agent rules when auth fails |
| `kiponos_status` | Connect + team/profile/root summary |
| `kiponos_get` | Read `folder/key` path |
| `kiponos_set` | Write path (live dashboard) |
| `kiponos_list` | Folders + keys at path |
| `kiponos_dump` | Nested JSON |
| `kiponos_mkdir` | Ensure folder path |
| `kiponos_delete_key` | Delete key |
| `kiponos_poll_events` | Drain live value-change events |

Heavy reactions to events: poll `kiponos_poll_events` in a loop or after user edits; do not block the MCP tool call for long.

## CLI fallback (no MCP)

```bash
export KIPONOS_ID=... KIPONOS_ACCESS=... KIPONOS="['app']['1.0']['dev']['base']"
kiponos-cli status
kiponos-cli get path/to/key
kiponos-cli set path/to/key value
```

## Install (user or agent)

From this kit root:

```bash
./install.sh                 # skill + pip package; prints MCP config snippet
./install.sh grok-user       # skill → ~/.grok/skills/kiponos-live
```

Grok MCP (`~/.grok/config.toml`):

```toml
[mcp_servers.kiponos]
command = "kiponos-mcp"
env = { KIPONOS_ID = "...", KIPONOS_ACCESS = "...", KIPONOS = "['my-app']['v1.0.0']['dev']['base']" }
enabled = true
```

## Design notes

- Paths use `/` (e.g. `marketing/press/dev-to-crunch/queue-progress`).
- `set` waits for server ack — never call from a raw listener thread without a worker queue.
- Java SDK remains the product model for Java apps (`/kiponos` integration skill); this kit is for **agents**.

## References

- [onboarding.md](references/onboarding.md)
- [mcp-tools.md](references/mcp-tools.md)
- Bundle root: `../README.md` and `../DESIGN.md`
