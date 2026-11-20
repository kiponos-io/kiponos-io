# Kiponos Agent Kit (free)

**Realtime config for AI agents** — Python SDK + MCP server + Agent Skill.

Dashboard and agents share one config tree over WebSocket. Change a value in the UI; the connected MCP sees it. Set a value from the agent; the dashboard updates without refresh.

| Component | What it is |
|-----------|------------|
| **SDK** `kiponos` | Python client v0.3 — get/set/list/mkdir/delete/on_change, auto-reconnect, offline backup, fail-closed reads |
| **MCP** `kiponos-mcp` | Long-lived tool process (stdio) |
| **Skill** `kiponos-live` | Onboarding + when to call tools |

## Install

```bash
pip install kiponos        # SDK (import kiponos) + kiponos-mcp + kiponos-cli
```

> Looking for the classic SDK? That is the separate `kiponos-pysdk` package (`from kiponos_pysdk import KiponosClient`).

To also install the agent skill, or to work from a checkout:

```bash
cd agent-kit   # this directory
./install.sh
```

Or manually from source:

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

## Connection resilience (0.3.0)

The client keeps the whole config tree in memory; `get()` is a local lookup.
Since 0.3.0 it also survives the network:

| State | Meaning |
|-------|---------|
| `CONNECTING` | first connect in progress |
| `READY` | connected; tree kept in sync by server push |
| `OFFLINE` | disconnected; serving the last config seen, reconnecting in the background |
| `SAFE_MODE` | like OFFLINE, but the server explicitly rejected the credentials (HTTP 401/403 or STOMP `ERROR`); still retrying at the max backoff |
| `CLOSED` | `close()` was called |

* **No idle cut-off.** 0.2.0 dropped the connection after 25 s without traffic. Fixed.
* **Auto-reconnect** (default on) with exponential backoff + jitter (0.5 s → 30 s).
  After each reconnect the full config is re-fetched (the server's normal bootstrap
  snapshot), and `on_change` fires with `source="resync"` for every key that was
  added or changed while offline. Keys deleted while offline are removed silently,
  like live deletes. `auto_reconnect=False` restores the 0.2.0 behaviour.
* **Keepalive.** A WebSocket ping every `ping_interval` (20 s). If the server has
  answered pings and then nothing arrives for `ping_interval + ping_timeout`
  (30 s), the link is treated as dead and the client reconnects. A server that
  never answers pings is not treated as dead.
* **Writes are never queued.** `set()` / `mkdir()` / `delete_key()` raise
  `ConnectionError` when not READY.

### Fail-closed reads (kill switches)

A cached value going stale silently is the wrong failure mode for a halt switch.
Every read can say how stale it may be:

```python
from kiponos import Kiponos

k = Kiponos.connect()

def trading_halted() -> bool:
    # True (halt) if the dashboard says so, if the key is missing or unparsable,
    # or if we have been disconnected for more than 5 seconds.
    return k.get_bool("trading/halt", default=True, max_staleness=5)
```

* `k.state`, `k.is_connected`, `k.on_state_change(cb(old, new))`, `k.wait_ready(timeout)`
* `k.staleness_seconds` – `0.0` while READY, else seconds since the last known sync
  (`inf` if never synced); `k.last_sync_time` – epoch seconds of that moment
* `k.get(path, default, max_staleness=N)` – `default` if staler than N seconds
* `k.require_fresh(N)` – raises `StaleConfigError`; `k.is_fresh(N)` – bool
* `max_staleness=0` means "only trust values while READY"

Limits: a silently dead network path is only noticed via the ping keepalive, so in
the worst case reads stay "fresh" for about `ping_interval + ping_timeout` after the
link died (30 s by default; lower `ping_interval` for tighter kill switches).

### Typed getters with range checks

```python
silence = k.get_float("voice/vad_min_silence", 0.55, min=0.05, max=3.0)
retries = k.get_int("http/retries", 3, min=0, max=10)
enabled = k.get_bool("features/new-ui", False)
```

They return `default` when the key is missing, empty, unparsable or outside
`[min, max]` (and with `max_staleness`, when stale). `get()` is unchanged and still
returns raw strings.

### Folder-scoped listeners

```python
k.on_change(lambda key, value, folders, source, delta: print(key, value), folder="voice")
```

### Last-known-good backup and offline startup (opt-in)

```python
k = Kiponos.connect(
    backup_path="/var/lib/myapp/kiponos-lkg.json",  # or env KIPONOS_BACKUP_PATH
    offline_startup=True,                            # or env KIPONOS_OFFLINE_STARTUP=1
)
```

* With `backup_path`, the full tree is written there (temp file + `os.replace`,
  mode 0600) after every load and change.
* With `offline_startup=True`, if the server can't be reached at startup the client
  starts `OFFLINE` from that backup and keeps reconnecting; `staleness_seconds`
  counts from the backup's save time, so fail-closed reads still fail closed.
* Both are **off by default**: without them, an unreachable server at startup raises
  `ConnectionError` exactly like 0.2.0, and nothing is written to disk (the backup
  holds your config values in plain text).
* A missing, corrupt, foreign-format or other-profile backup is ignored, and
  startup raises `ConnectionError`. Rejected credentials (`AuthError`) always raise.

### All constructor options

`auto_reconnect=True`, `backup_path=None`, `offline_startup=False`,
`connect_timeout=25`, `ping_interval=20` (`None` disables), `ping_timeout=10`,
`reconnect_min_delay=0.5`, `reconnect_max_delay=30`, plus the 0.2.0 options
`server_url`, `kiponos_id`, `kiponos_access`, `quiet`, `request_timeout`.

## Docs

- [DESIGN.md](DESIGN.md) — architecture Skill / MCP / SDK  
- [CHANGELOG.md](CHANGELOG.md)  
- [skill/references/onboarding.md](skill/references/onboarding.md)  
- Homepage: https://kiponos.io  

## License

Apache-2.0
