#!/usr/bin/env python3
"""Kiponos MCP server (stdio).

Keeps one ``Kiponos`` SDK connection for the process lifetime and exposes tools
agents call without embedding wire protocol knowledge.

Env (required after onboarding):
  KIPONOS_ID, KIPONOS_ACCESS, KIPONOS (profile bracket path)

Optional:
  KIPONOS_SERVER_URL  default wss://kiponos.io/api/io-kiponos-sdk
"""

from __future__ import annotations

import json
import os
import queue
import threading
from typing import Any, Optional

from mcp.server.fastmcp import FastMCP

from kiponos import Kiponos, KiponosError
from kiponos.client import DEFAULT_SERVER

mcp = FastMCP(
    "kiponos",
    instructions=(
        "Live Kiponos.io config hub. Use kiponos_status / get / set for realtime "
        "config. If not connected, guide the user through onboarding "
        "(https://kiponos.io → free account → Connect → tokens + profile). "
        "Never print full token values."
    ),
)

_client: Optional[Kiponos] = None
_client_lock = threading.Lock()
_event_q: queue.Queue = queue.Queue(maxsize=200)
_listener_attached = False


def _get_client() -> Kiponos:
    global _client, _listener_attached
    with _client_lock:
        if _client is not None and _client.is_connected:
            return _client
        if _client is not None:
            try:
                _client.close()
            except Exception:
                pass
            _client = None
            _listener_attached = False

        profile = os.environ.get("KIPONOS")
        server = os.environ.get("KIPONOS_SERVER_URL", DEFAULT_SERVER)
        client = Kiponos(profile=profile, server_url=server, quiet=True)
        client.open()

        def on_change(key, value, folders=(), source="", delta=None):
            try:
                _event_q.put_nowait(
                    {
                        "key": key,
                        "value": value,
                        "path": "/".join(list(folders) + [key]) if key else "/".join(folders),
                        "folders": list(folders),
                        "source": source,
                    }
                )
            except queue.Full:
                try:
                    _event_q.get_nowait()
                except queue.Empty:
                    pass
                try:
                    _event_q.put_nowait(
                        {
                            "key": key,
                            "value": value,
                            "path": "/".join(list(folders) + [key]),
                            "folders": list(folders),
                            "source": source,
                        }
                    )
                except queue.Full:
                    pass

        client.on_change(on_change)
        _listener_attached = True
        _client = client
        return _client


def _err(e: Exception) -> str:
    return json.dumps({"ok": False, "error": type(e).__name__, "message": str(e)})


@mcp.tool()
def kiponos_onboarding() -> str:
    """Return step-by-step onboarding for Kiponos tokens + profile. Call when connect fails or credentials missing."""
    return json.dumps(
        {
            "ok": True,
            "steps": [
                {
                    "n": 1,
                    "title": "Create or log in",
                    "detail": "Open https://kiponos.io and create a free TeamPro account (or log in).",
                },
                {
                    "n": 2,
                    "title": "Open Connect",
                    "detail": (
                        "In the dashboard: Connect Your App (or Configs). "
                        "Create/select app, release, environment, and config profile."
                    ),
                },
                {
                    "n": 3,
                    "title": "Copy tokens",
                    "detail": (
                        "Copy KIPONOS_ID and KIPONOS_ACCESS (long eyJ… JWEs). "
                        "Never commit them to git or paste into public chats."
                    ),
                },
                {
                    "n": 4,
                    "title": "Copy profile path",
                    "detail": (
                        "Copy the bracket path exactly, e.g. "
                        "['my-app']['v1.0.0']['dev']['base']. "
                        "Tokens and profile must belong to the same connection."
                    ),
                },
                {
                    "n": 5,
                    "title": "Export env and retry",
                    "detail": (
                        "export KIPONOS_ID=...\nexport KIPONOS_ACCESS=...\n"
                        "export KIPONOS=\"['my-app']['v1.0.0']['dev']['base']\"\n"
                        "Then call kiponos_status or restart the MCP with those env vars."
                    ),
                },
            ],
            "agent_rules": [
                "Never invent tokens.",
                "Handshake 500 usually means tokens/profile mismatch, not missing network.",
                "After env is set, use kiponos_status to verify connect: OK.",
            ],
        },
        indent=2,
    )


@mcp.tool()
def kiponos_status() -> str:
    """Connect (if needed) and return team id, profile, root keys/folders count."""
    try:
        k = _get_client()
        return json.dumps(
            {
                "ok": True,
                "connected": k.is_connected,
                "team_id": k.team_id,
                "profile": k.profile,
                "root_keys": k.list_keys(),
                "root_folders": k.list_folders(),
            },
            indent=2,
        )
    except KiponosError as e:
        return json.dumps(
            {
                "ok": False,
                "error": type(e).__name__,
                "message": str(e),
                "hint": "Call kiponos_onboarding and help the user set env vars.",
            },
            indent=2,
        )


@mcp.tool()
def kiponos_get(path: str) -> str:
    """Get a config value by path (e.g. marketing/press/dev-to-crunch/queue-progress)."""
    try:
        k = _get_client()
        v = k.get(path)
        return json.dumps({"ok": True, "path": path, "value": v})
    except Exception as e:
        return _err(e)


@mcp.tool()
def kiponos_set(path: str, value: str) -> str:
    """Set a config value by path (creates parent folders). Live dashboard update."""
    try:
        k = _get_client()
        out = k.set(path, value)
        return json.dumps({"ok": True, "path": path, "value": out})
    except Exception as e:
        return _err(e)


@mcp.tool()
def kiponos_list(path: str = "") -> str:
    """List immediate folders and keys under path (empty = profile root)."""
    try:
        k = _get_client()
        return json.dumps(
            {
                "ok": True,
                "path": path or "/",
                "folders": k.list_folders(path),
                "keys": k.list_keys(path),
            },
            indent=2,
        )
    except Exception as e:
        return _err(e)


@mcp.tool()
def kiponos_dump(path: str = "") -> str:
    """Dump nested JSON tree under path (empty = full profile tree)."""
    try:
        k = _get_client()
        return json.dumps({"ok": True, "path": path or "/", "tree": k.dump(path)}, indent=2)
    except Exception as e:
        return _err(e)


@mcp.tool()
def kiponos_mkdir(path: str) -> str:
    """Create folder path (folderOrCreate-style ensure)."""
    try:
        k = _get_client()
        k.ensure_path(path)
        return json.dumps({"ok": True, "path": path})
    except Exception as e:
        return _err(e)


@mcp.tool()
def kiponos_delete_key(path: str) -> str:
    """Delete a key at path (not a folder)."""
    try:
        k = _get_client()
        out = k.delete_key(path)
        return json.dumps({"ok": True, "path": out})
    except Exception as e:
        return _err(e)


@mcp.tool()
def kiponos_poll_events(max_events: int = 20) -> str:
    """Drain recent value-change events from the live subscription (non-blocking)."""
    events: list[dict[str, Any]] = []
    n = max(1, min(int(max_events), 100))
    for _ in range(n):
        try:
            events.append(_event_q.get_nowait())
        except queue.Empty:
            break
    return json.dumps({"ok": True, "count": len(events), "events": events}, indent=2)


def main() -> None:
    mcp.run(transport="stdio")


if __name__ == "__main__":
    main()
