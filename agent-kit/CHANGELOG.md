# Changelog — `kiponos` (Python SDK + MCP)

## 0.3.0 — unreleased

Backward compatible: every 0.2.0 call keeps its signature and default behaviour,
except the improvements marked *(behaviour change)*.

### Fixed
- **Idle disconnect.** The 25 s handshake timeout also applied to the listener's
  `recv()`, so a quiet connection died after 25 s and stayed dead. The socket
  timeout is now cleared after the handshake/bootstrap.
- `close()` called from an `on_change` callback (the listener thread) no longer
  raises `RuntimeError: cannot join current thread`.
- A failed handshake/bootstrap no longer leaves the socket open.
- Low-level socket errors during CONNECT/bootstrap are raised as
  `kiponos.ConnectionError` instead of raw `websocket` exceptions.

### Added
- Automatic reconnect with exponential backoff + jitter (`auto_reconnect=True`,
  `reconnect_min_delay=0.5`, `reconnect_max_delay=30`), re-subscribe, and a full
  re-sync after each reconnect: `on_change` fires with `source="resync"` for keys
  added or changed while offline *(behaviour change: 0.2.0 stayed disconnected)*.
- Connection states `ConnectionState.CONNECTING / READY / OFFLINE / SAFE_MODE /
  CLOSED`: `state`, `on_state_change(cb)`, `wait_ready(timeout)`, `reconnect_count`,
  `last_error`. `is_connected` is kept (True only in READY).
- WebSocket ping keepalive (`ping_interval=20`, `ping_timeout=10`) with dead-link
  detection once the server has answered a ping.
- Staleness and fail-closed reads: `staleness_seconds`, `last_sync_time`,
  `is_fresh(n)`, `require_fresh(n)` → `StaleConfigError`, and
  `get(path, default, max_staleness=n)`.
- Typed getters `get_int`, `get_float`, `get_bool` with optional `min`/`max`
  (out-of-range, unparsable or empty → `default`) and `max_staleness`.
- `on_change(cb, folder="a/b")` folder-scoped listeners; `on_change` returns `cb`.
- Opt-in last-known-good backup (`backup_path=` / `KIPONOS_BACKUP_PATH`), written
  atomically with mode 0600 after each load and change; `flush_backup()`.
- Opt-in offline startup from that backup (`offline_startup=True` /
  `KIPONOS_OFFLINE_STARTUP=1`). Corrupt / foreign / other-profile backups are
  ignored and startup raises `ConnectionError` as before.
- `AuthError` (subclass of `ConnectionError`) for HTTP 401/403 or a STOMP `ERROR`
  reply to CONNECT.
- A request waiting for its server ack (`set`, `mkdir`, `delete_key`) now fails
  with `ConnectionError` as soon as the connection drops, instead of waiting for
  the full `request_timeout` *(behaviour change)*.
- `kiponos-cli status` reports `state`.

### Wire protocol
- Unchanged STOMP frames and order (CONNECT, SUBSCRIBE `/user/queue/sdk-boot`,
  then the five team topics). New on the wire: WebSocket-level ping control
  frames every `ping_interval` seconds (`ping_interval=None` disables them).

## 0.2.0
- First release of the agent kit SDK (`get/set/list/mkdir/delete/on_change`) and
  `kiponos-mcp`.
