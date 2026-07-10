# Kiponos Agent Kit (free)

**Realtime config for AI agents** — Python SDK + MCP server + Agent Skill.

Dashboard and agents share one config tree over WebSocket. Change a value in the UI; the connected MCP sees it. Set a value from the agent; the dashboard updates without refresh.

| Component | What it is |
|-----------|------------|
| **SDK** `kiponos` | Python client v0.2 — get/set/list/mkdir/delete/on_change |
| **MCP** `kiponos-mcp` | Long-lived tool process (stdio) |
| **Skill** `kiponos-live` | Onboarding + when to call tools |

## Install

```bash
cd agent-kit   # this directory
./install.sh
```

Or manually:

```bash
pip install -e .
./install.sh grok-user   # skill only
```

### Grok MCP (`~/.grok/config.toml`)

```toml
[mcp_servers.kiponos]
command = "kiponos-mcp"
env = {
  KIPONOS_ID = "eyJ…",
  KIPONOS_ACCESS = "eyJ…",
  KIPONOS = "['my-app']['v1.0.0']['dev']['base']"
}
enabled = true
```

Get tokens + profile from https://kiponos.io → **Connect**.

### Cursor / Claude

Install skill via `./install.sh cursor-user` or `claude-user`.  
Add the same stdio MCP command `kiponos-mcp` with env in your MCP settings.

## Quick SDK use

```python
from kiponos import Kiponos

with Kiponos.connect() as k:  # uses env KIPONOS*
    k.set("demo/hello", "from-agent")
    print(k.get("demo/hello"))
```

```bash
kiponos-cli status
kiponos-cli set demo/hello world
```

## Docs

- [DESIGN.md](DESIGN.md) — architecture Skill / MCP / SDK  
- [skill/references/onboarding.md](skill/references/onboarding.md)  
- Homepage: https://kiponos.io  

## License

Apache-2.0
