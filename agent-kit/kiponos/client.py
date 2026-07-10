"""Kiponos realtime client — WebSocket/STOMP, nested get/set, change events.

Wire protocol aligned with Java SDK ReadyMode (sdk-boot-3).
"""

from __future__ import annotations

import json
import os
import re
import threading
import time
import uuid
import zlib
from typing import Any, Callable, Optional
from urllib.parse import urlparse

from .paths import VALUE_KEY, join_json_path, parse_path, profile_to_base_path

try:
    import websocket
except ImportError as e:  # pragma: no cover
    raise ImportError(
        "websocket-client required: pip install websocket-client"
    ) from e

DEFAULT_SERVER = "wss://kiponos.io/api/io-kiponos-sdk"
SDK_VERSION = "4.2.Esther"
DEFAULT_TIMEOUT = 30.0

# Callback: (key, value, folders: tuple[str,...], source: str, delta: dict)
ChangeHandler = Callable[..., None]


class KiponosError(Exception):
    pass


class ConnectionError(KiponosError):
    pass


class TimeoutError(KiponosError):
    pass


class Kiponos:
    """Live config hub client.

    Auth: env ``KIPONOS_ID`` + ``KIPONOS_ACCESS`` (or constructor).
    Profile: env ``KIPONOS`` or ``profile=`` bracket path.
    """

    Q_BOOTSTRAP = "/user/queue/sdk-boot"

    def __init__(
        self,
        profile: Optional[str] = None,
        *,
        server_url: str = DEFAULT_SERVER,
        kiponos_id: Optional[str] = None,
        kiponos_access: Optional[str] = None,
        quiet: bool = True,
        request_timeout: float = DEFAULT_TIMEOUT,
    ):
        self.server_url = server_url or DEFAULT_SERVER
        self.quiet = quiet
        self.request_timeout = request_timeout
        self.kiponos_id = kiponos_id or os.environ.get("KIPONOS_ID")
        self.kiponos_access = kiponos_access or os.environ.get("KIPONOS_ACCESS")
        if not self.kiponos_id or not self.kiponos_access:
            raise KiponosError(
                "Missing KIPONOS_ID and/or KIPONOS_ACCESS. "
                "Create a free account at https://kiponos.io → Connect → copy tokens."
            )
        self.profile = profile or os.environ.get("KIPONOS")
        if not self.profile:
            raise KiponosError(
                "Missing config profile. Set env KIPONOS or pass profile= "
                "e.g. ['my-app']['v1.0.0']['dev']['base'] from the Connect screen."
            )
        self.base_path = profile_to_base_path(self.profile)
        self.ws: Optional[websocket.WebSocket] = None
        self.config_tree: dict = {}
        self.team_info: dict = {}
        self.team_id: str = ""
        self.last_sub_id = 200
        self.handlers: dict[str, Callable] = {}
        self.listener_thread: Optional[threading.Thread] = None
        self.is_connected = False
        self._lock = threading.RLock()
        self._pending: dict[str, threading.Event] = {}
        self._pending_result: dict[str, Any] = {}
        self._on_change: list[ChangeHandler] = []

    # --- factory ---

    @classmethod
    def connect(
        cls,
        profile: Optional[str] = None,
        **kwargs: Any,
    ) -> "Kiponos":
        client = cls(profile=profile, **kwargs)
        client.open()
        return client

    def open(self) -> "Kiponos":
        return self._connect()

    def _log(self, msg: str) -> None:
        if not self.quiet:
            print(msg, flush=True)

    def _headers(self) -> list[str]:
        return [
            f"sdk-id-token: {self.kiponos_id}",
            f"sdk-access-token: {self.kiponos_access}",
            f"kiponos-id: {self.profile}",
            f"sdk-version: {SDK_VERSION}",
        ]

    # --- events ---

    def on_change(self, callback: ChangeHandler) -> None:
        """Register ``callback(key, value, folders, source, delta)``.

        Prefer running heavy work off the listener thread (queue/worker).
        """
        self._on_change.append(callback)

    def _emit(
        self, key: str, value: Any, folders: list[str], source: str, delta: dict
    ) -> None:
        for cb in list(self._on_change):
            try:
                cb(key, value, tuple(folders), source, delta)
            except TypeError:
                try:
                    cb(key, value)
                except Exception as e:
                    self._log(f"on_change error: {e}")
            except Exception as e:
                self._log(f"on_change error: {e}")

    def _folders_from_base(self, base: str) -> list[str]:
        if not base:
            return []
        rel = base
        if rel.startswith(self.base_path):
            rel = rel[len(self.base_path) :]
        return re.findall(r"\['([^']*)'\]", rel)

    # --- tree ---

    def _resolve_node(self, folders: list[str]) -> dict:
        node: Any = self.config_tree
        for f in folders:
            if not isinstance(node, dict) or f not in node:
                raise KeyError(f"Folder not found: {'/'.join(folders)} (at {f!r})")
            node = node[f]
            if isinstance(node, dict) and VALUE_KEY in node and len(node) == 1:
                raise KeyError(f"Path hits a key, not a folder: {f}")
        if not isinstance(node, dict):
            raise KeyError(f"Not a folder: {'/'.join(folders)}")
        return node

    def _is_key_node(self, node: Any) -> bool:
        return isinstance(node, dict) and VALUE_KEY in node

    def _apply_value_at(self, folders: list[str], key: str, value: Any) -> None:
        with self._lock:
            try:
                parent = self._resolve_node(folders) if folders else self.config_tree
            except KeyError:
                parent = self.config_tree
                for f in folders:
                    if f not in parent or self._is_key_node(parent.get(f)):
                        parent[f] = {}
                    parent = parent[f]
            parent[key] = {VALUE_KEY: value}

    def get(self, path: str, default: Any = None) -> Any:
        """Get value by ``folder/sub/key`` path (or root key)."""
        parts = parse_path(path)
        if not parts:
            return default
        key, folders = parts[-1], parts[:-1]
        with self._lock:
            try:
                parent = self._resolve_node(folders) if folders else self.config_tree
            except KeyError:
                return default
            node = parent.get(key)
            if self._is_key_node(node):
                return node.get(VALUE_KEY, default)
            return default

    def list_keys(self, path: str = "") -> list[str]:
        folders = parse_path(path)
        with self._lock:
            parent = self._resolve_node(folders) if folders else self.config_tree
            return sorted(k for k, v in parent.items() if self._is_key_node(v))

    def list_folders(self, path: str = "") -> list[str]:
        folders = parse_path(path)
        with self._lock:
            parent = self._resolve_node(folders) if folders else self.config_tree
            return sorted(
                k
                for k, v in parent.items()
                if isinstance(v, dict) and not self._is_key_node(v)
            )

    def dump(self, path: str = "") -> dict:
        folders = parse_path(path)
        with self._lock:
            if not folders:
                return json.loads(json.dumps(self.config_tree))
            return json.loads(json.dumps(self._resolve_node(folders)))

    # --- connect ---

    def _connect(self) -> "Kiponos":
        self.ws = websocket.WebSocket()
        try:
            self.ws.connect(self.server_url, header=self._headers(), timeout=25)
            self.is_connected = True
        except Exception as e:
            self.is_connected = False
            raise ConnectionError(
                f"WebSocket handshake failed: {e}. "
                "Check tokens match the profile (Connect screen)."
            ) from e

        host = urlparse(self.server_url).hostname or "kiponos.io"
        self.ws.send(f"CONNECT\naccept-version:1.2\nhost:{host}\n\n\0")
        response = self.ws.recv()
        if not isinstance(response, str) or not response.startswith("CONNECTED"):
            self.is_connected = False
            raise ConnectionError(f"STOMP CONNECT failed: {response!r}")

        self.ws.send(f"SUBSCRIBE\nid:0\ndestination:{self.Q_BOOTSTRAP}\n\n\0")
        boot_msg = self.ws.recv()
        if not isinstance(boot_msg, bytes):
            raise KiponosError(f"Expected binary bootstrap, got {type(boot_msg)}")

        command, _headers, body = self._parse_stomp_frame(boot_msg)
        if command != "MESSAGE":
            raise KiponosError(f"Expected MESSAGE frame, got {command}")

        decompressed = zlib.decompress(body, wbits=-zlib.MAX_WBITS)
        json_arr = json.loads(decompressed.decode("utf-8"))
        self.config_tree = json_arr[0] if len(json_arr) > 0 else {}
        self.team_info = json_arr[1] if len(json_arr) > 1 else {}
        self.team_id = self.team_info.get("teamId") or ""
        if not self.team_id:
            raise KiponosError("Bootstrap missing teamId")

        self._log(f"Connected team={self.team_id} profile={self.profile}")

        self._subs_topic("config-key-created", self._on_key_created)
        self._subs_topic("config-val-updated", self._on_val_updated)
        self._subs_topic("config-prop-saved", self._on_prop_saved)
        self._subs_topic("config-folder-created", self._on_folder_created)
        self._subs_topic("config-key-deleted", self._on_key_deleted)
        time.sleep(0.35)

        self.listener_thread = threading.Thread(target=self._main_listener, daemon=True)
        self.listener_thread.start()
        return self

    def _subs_topic(self, topic_name: str, handler: Callable) -> None:
        self.last_sub_id += 1
        dest = f"/topic/team/{self.team_id}/{topic_name}"
        frame = f"SUBSCRIBE\nid:{self.last_sub_id}\ndestination:{dest}\n\n\0"
        self.handlers[str(self.last_sub_id)] = handler
        assert self.ws is not None
        self.ws.send(frame)

    def _parse_stomp_frame(self, data: bytes):
        header_end = data.find(b"\n\n")
        if header_end == -1:
            raise KiponosError("No header end in STOMP frame")
        header_bytes = data[:header_end]
        body = data[header_end + 2 :]
        lines = header_bytes.decode("utf-8").split("\n")
        command = lines[0].strip()
        headers: dict[str, str] = {}
        for line in lines[1:]:
            if line.strip() and ":" in line:
                k, v = line.split(":", 1)
                headers[k.strip()] = v.strip()
        if body.endswith(b"\0"):
            body = body[:-1]
        return command, headers, body

    def _main_listener(self) -> None:
        while self.ws and self.is_connected:
            try:
                msg = self.ws.recv()
                if not isinstance(msg, str) or not msg.startswith("MESSAGE"):
                    continue
                header_end = msg.find("\n\n")
                if header_end == -1:
                    continue
                header_str = msg[:header_end]
                body = msg[header_end + 2 :].rstrip("\0")
                headers: dict[str, str] = {}
                for line in header_str.split("\n")[1:]:
                    if line.strip() and ":" in line:
                        k, v = line.split(":", 1)
                        headers[k.strip()] = v.strip()
                sub_id = headers.get("subscription")
                handler = self.handlers.get(sub_id or "")
                if not handler:
                    continue
                try:
                    delta = json.loads(body) if body else {}
                except json.JSONDecodeError:
                    continue
                try:
                    handler(delta)
                except Exception as e:
                    self._log(f"handler error: {e}")
            except websocket.WebSocketConnectionClosedException:
                self.is_connected = False
                break
            except Exception as e:
                self._log(f"listener error: {e}")
                self.is_connected = False
                break

    def _on_key_created(self, delta: dict) -> None:
        key = delta.get("key")
        if not key:
            return
        folders = self._folders_from_base(delta.get("basePath") or "")
        self._apply_value_at(folders, key, "")
        self._emit(key, "", folders, "config-key-created", delta)

    def _on_val_updated(self, delta: dict) -> None:
        key = delta.get("key")
        value = delta.get("value")
        if key is None:
            return
        folders = self._folders_from_base(delta.get("basePath") or "")
        self._apply_value_at(folders, key, value)
        self._emit(key, value, folders, "config-val-updated", delta)

    def _on_prop_saved(self, delta: dict) -> None:
        req_id = delta.get("requestId")
        key = delta.get("key")
        value = delta.get("value")
        base = delta.get("basePath") or ""
        if key is not None:
            folders = self._folders_from_base(base)
            self._apply_value_at(folders, key, value)
            if req_id and req_id in self._pending:
                self._pending_result[req_id] = value
                self._pending[req_id].set()
            self._emit(key, value, folders, "config-prop-saved", delta)

    def _on_folder_created(self, delta: dict) -> None:
        req_id = delta.get("requestId")
        folder = delta.get("folder") or delta.get("folderName")
        path = delta.get("path") or delta.get("basePath") or ""
        with self._lock:
            if folder:
                folders = self._folders_from_base(path)
                try:
                    parent = self._resolve_node(folders) if folders else self.config_tree
                    parent.setdefault(folder, {})
                except KeyError:
                    pass
        if req_id and req_id in self._pending:
            self._pending_result[req_id] = folder
            self._pending[req_id].set()

    def _on_key_deleted(self, delta: dict) -> None:
        req_id = delta.get("requestId")
        key = delta.get("key")
        base = delta.get("basePath") or ""
        folders = self._folders_from_base(base)
        with self._lock:
            if key is not None:
                try:
                    parent = self._resolve_node(folders) if folders else self.config_tree
                    parent.pop(key, None)
                except KeyError:
                    if not folders:
                        self.config_tree.pop(key, None)
        if req_id and req_id in self._pending:
            self._pending_result[req_id] = key
            self._pending[req_id].set()

    def _new_request_id(self) -> str:
        return f"{int(time.time() * 1000)}-{uuid.uuid4().hex[:8]}"

    def _send_json(self, destination: str, payload: dict) -> None:
        if not self.ws or not self.is_connected:
            raise ConnectionError("Not connected")
        body = json.dumps(payload, separators=(",", ":"))
        frame = (
            f"SEND\n"
            f"destination:{destination}\n"
            f"content-type:application/json\n"
            f"content-length:{len(body.encode('utf-8'))}\n"
            f"\n"
            f"{body}\0"
        )
        self.ws.send(frame)

    def _wait(self, request_id: str, timeout: Optional[float] = None) -> Any:
        ev = threading.Event()
        self._pending[request_id] = ev
        try:
            ok = ev.wait(timeout if timeout is not None else self.request_timeout)
            if not ok:
                raise TimeoutError(
                    f"No server response for request {request_id} within timeout"
                )
            return self._pending_result.get(request_id)
        finally:
            self._pending.pop(request_id, None)
            self._pending_result.pop(request_id, None)

    def set(self, path: str, value: str) -> str:
        """Create/update key at path (creates parent folders). Returns confirmed value.

        Do not call from the STOMP listener thread without a worker queue
        (``set`` waits for ack on the same connection).
        """
        parts = parse_path(path)
        if not parts:
            raise ValueError("empty path")
        key, folders = parts[-1], parts[:-1]
        value = "" if value is None else str(value)
        if folders:
            self.ensure_path("/".join(folders))
        base = join_json_path(self.base_path, *folders)

        with self._lock:
            try:
                parent = self._resolve_node(folders) if folders else self.config_tree
                exists = key in parent and self._is_key_node(parent.get(key))
                old_key = key if exists else None
                if exists and parent[key].get(VALUE_KEY) == value:
                    return value
            except KeyError:
                old_key = None

        request_id = self._new_request_id()
        self._send_json(
            "/app/sdk-save-config-prop",
            {
                "requestId": request_id,
                "basePath": base,
                "key": key,
                "value": value,
                "oldKey": old_key,
            },
        )
        result = self._wait(request_id)
        return result if result is not None else value

    def mkdir(self, path: str) -> str:
        parts = parse_path(path)
        if not parts:
            raise ValueError("empty path")
        if len(parts) > 1:
            self.ensure_path("/".join(parts[:-1]))
        folder_name = parts[-1]
        parent_folders = parts[:-1]
        parent_path = join_json_path(self.base_path, *parent_folders)
        with self._lock:
            try:
                parent = (
                    self._resolve_node(parent_folders)
                    if parent_folders
                    else self.config_tree
                )
                if folder_name in parent and not self._is_key_node(parent[folder_name]):
                    return folder_name
            except KeyError:
                pass
        request_id = self._new_request_id()
        self._send_json(
            "/app/sdk-create-config-folder",
            {
                "requestId": request_id,
                "path": parent_path,
                "folder": folder_name,
            },
        )
        result = self._wait(request_id)
        with self._lock:
            try:
                parent = (
                    self._resolve_node(parent_folders)
                    if parent_folders
                    else self.config_tree
                )
                parent.setdefault(folder_name, {})
            except KeyError:
                pass
        return result or folder_name

    def ensure_path(self, path: str) -> None:
        parts = parse_path(path)
        built: list[str] = []
        for f in parts:
            parent = list(built)
            with self._lock:
                try:
                    pnode = self._resolve_node(parent) if parent else self.config_tree
                    if f in pnode and not self._is_key_node(pnode[f]):
                        built.append(f)
                        continue
                except KeyError:
                    pass
            self.mkdir("/".join(built + [f]) if built else f)
            built.append(f)

    def delete_key(self, path: str) -> str:
        parts = parse_path(path)
        if not parts:
            raise ValueError("empty path")
        key, folders = parts[-1], parts[:-1]
        base = join_json_path(self.base_path, *folders)
        with self._lock:
            try:
                parent = self._resolve_node(folders) if folders else self.config_tree
                if key not in parent or not self._is_key_node(parent.get(key)):
                    return key
            except KeyError:
                return key
        request_id = self._new_request_id()
        self._send_json(
            "/app/delete-config-key",
            {"requestId": request_id, "basePath": base, "key": key},
        )
        try:
            self._wait(request_id, timeout=min(self.request_timeout, 20.0))
        except TimeoutError:
            self._send_json(
                "/app/delete-config-prop",
                {"basePath": base, "key": key},
            )
            time.sleep(0.4)
        with self._lock:
            try:
                parent = self._resolve_node(folders) if folders else self.config_tree
                parent.pop(key, None)
            except KeyError:
                pass
        return key

    def close(self) -> None:
        self.is_connected = False
        if self.ws:
            try:
                self.ws.send("DISCONNECT\n\n\0")
            except Exception:
                pass
            try:
                self.ws.close()
            except Exception:
                pass
            self.ws = None
        if self.listener_thread and self.listener_thread.is_alive():
            self.listener_thread.join(timeout=3)

    def __enter__(self) -> "Kiponos":
        if not self.is_connected:
            self.open()
        return self

    def __exit__(self, *args) -> None:
        self.close()
