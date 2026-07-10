# MCP tools reference

Process: `kiponos-mcp` (stdio). One `Kiponos` client per process.

## Lifecycle

```
agent starts MCP → first tool call → connect WebSocket → tools use shared client
```

Events from team topics are buffered; drain with `kiponos_poll_events`.

## Tool summary

| Name | Args | Notes |
|------|------|--------|
| kiponos_onboarding | — | No network required |
| kiponos_status | — | Connects if needed |
| kiponos_get | path | null if missing |
| kiponos_set | path, value | Creates parents |
| kiponos_list | path? | Root if empty |
| kiponos_dump | path? | Full subtree JSON |
| kiponos_mkdir | path | ensure_path |
| kiponos_delete_key | path | Key only |
| kiponos_poll_events | max_events? | Non-blocking drain |

## Error shape

```json
{"ok": false, "error": "ConnectionError", "message": "..."}
```

If auth-related, call `kiponos_onboarding` next.
