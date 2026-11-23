"""Kiponos realtime client — WebSocket/STOMP, nested get/set, change events.

Wire protocol aligned with Java SDK ReadyMode (sdk-boot-3).

0.3.0 adds (all backward compatible):

* no idle cut-off: the 25 s handshake timeout no longer applies to the
  listener's ``recv()``;
* automatic reconnect with exponential backoff + jitter, re-subscribe, and a
  full re-sync (the server's bootstrap snapshot) after every reconnect, with
  ``on_change`` fired for keys whose value differs;
* connection states (CONNECTING / READY / OFFLINE / SAFE_MODE / CLOSED) with
  ``state`` and ``on_state_change``;
* opt-in last-known-good backup file and opt-in offline startup from it;
* staleness tracking and fail-closed reads (``max_staleness=``,
  ``require_fresh()``, ``is_fresh()``);
* typed getters with optional range checks and folder-scoped listeners.
"""

from __future__ import annotations

import enum
import json
import logging
import math
import os
import random
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
DEFAULT_CONNECT_TIMEOUT = 25.0
DEFAULT_PING_INTERVAL = 20.0
DEFAULT_PING_TIMEOUT = 10.0
DEFAULT_RECONNECT_MIN_DELAY = 0.5
DEFAULT_RECONNECT_MAX_DELAY = 30.0
BACKUP_FORMAT = "kiponos-lkg"
BACKUP_FORMAT_VERSION = 1

log = logging.getLogger("kiponos")
log.addHandler(logging.NullHandler())

# Callback: (key, value, folders: tuple[str,...], source: str, delta: dict)
ChangeHandler = Callable[..., None]
# Callback: (old_state: ConnectionState, new_state: ConnectionState)
StateHandler = Callable[["ConnectionState", "ConnectionState"], None]

_TRUE = {"true", "1", "yes", "y", "on"}
_FALSE = {"false", "0", "no", "n", "off"}


class KiponosError(Exception):
    pass


class ConnectionError(KiponosError):
    pass


class AuthError(ConnectionError):
    """The server explicitly rejected the credentials (HTTP 401/403 or a
    STOMP ERROR frame in reply to CONNECT)."""


class TimeoutError(KiponosError):
    pass


class StaleConfigError(KiponosError):
    """Raised by :meth:`Kiponos.require_fresh` when the local copy is too old."""


class ConnectionState(str, enum.Enum):
    """Connection state, modelled on the Java SDK modes.

    * ``CONNECTING`` – first connect in progress, no config yet.
    * ``READY`` – connected; the tree is kept in sync by server push.
    * ``OFFLINE`` – not connected; serving the last config seen (in memory or
      from the backup file) while reconnecting in the background.
    * ``SAFE_MODE`` – like OFFLINE, but the server explicitly rejected the
      credentials on the last attempt (HTTP 401/403 or STOMP ERROR). Still
      retrying, at the maximum backoff delay.
    * ``CLOSED`` – ``close()`` was called.
    """

    CONNECTING = "CONNECTING"
    READY = "READY"
    OFFLINE = "OFFLINE"
    SAFE_MODE = "SAFE_MODE"
    CLOSED = "CLOSED"

    def __str__(self) -> str:  # pragma: no cover - cosmetic
        return self.value


def _env_flag(name: str) -> bool:
    return (os.environ.get(name) or "").strip().lower() in _TRUE


class Kiponos:
    """Live config hub client.

    Auth: env ``KIPONOS_ID`` + ``KIPONOS_ACCESS`` (or constructor).
    Profile: env ``KIPONOS`` or ``profile=`` bracket path.

    New in 0.3.0 (keyword-only, all optional):

    * ``auto_reconnect`` (default ``True``): reconnect with backoff after a drop.
    * ``backup_path`` (default ``None`` / env ``KIPONOS_BACKUP_PATH``): write the
      full config tree there (atomically) after every load and change.
    * ``offline_startup`` (default ``False`` / env ``KIPONOS_OFFLINE_STARTUP=1``):
      if the server can't be reached at startup, start OFFLINE from the backup
      instead of raising. Without a usable backup ``ConnectionError`` is still
      raised. Rejected credentials (``AuthError``) always raise.
    * ``ping_interval`` / ``ping_timeout``: WebSocket ping keepalive and dead
      connection detection (``0`` / ``None`` disables pings).
    * ``reconnect_min_delay`` / ``reconnect_max_delay``: backoff bounds.
    * ``connect_timeout``: handshake + bootstrap timeout (was a fixed 25 s).
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
        auto_reconnect: bool = True,
        backup_path: Optional[str] = None,
        offline_startup: Optional[bool] = None,
        connect_timeout: float = DEFAULT_CONNECT_TIMEOUT,
        ping_interval: Optional[float] = DEFAULT_PING_INTERVAL,
        ping_timeout: float = DEFAULT_PING_TIMEOUT,
        reconnect_min_delay: float = DEFAULT_RECONNECT_MIN_DELAY,
        reconnect_max_delay: float = DEFAULT_RECONNECT_MAX_DELAY,
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

        # --- 0.3.0 ---
        self.auto_reconnect = auto_reconnect
        self.backup_path: Optional[str] = (
            backup_path or os.environ.get("KIPONOS_BACKUP_PATH") or None
        )
        if offline_startup is None:
            offline_startup = _env_flag("KIPONOS_OFFLINE_STARTUP")
        self.offline_startup = bool(offline_startup)
        if self.offline_startup and not self.backup_path:
            raise KiponosError(
                "offline_startup=True needs backup_path= (or env KIPONOS_BACKUP_PATH): "
                "there is nothing to start from without a last-known-good backup."
            )
        self.connect_timeout = connect_timeout
        self.ping_interval = ping_interval if ping_interval else None
        self.ping_timeout = ping_timeout
        self.reconnect_min_delay = max(0.01, reconnect_min_delay)
        self.reconnect_max_delay = max(self.reconnect_min_delay, reconnect_max_delay)

        self._state = ConnectionState.CONNECTING
        self._state_handlers: list[StateHandler] = []
        self._state_cond = threading.Condition()
        self._closing = threading.Event()
        self._synced_at: Optional[float] = None  # epoch; last moment known in sync
        self._last_rx_mono = time.monotonic()
        self._last_rx_epoch = time.time()
        self._liveness_verified = False  # server answered a ping on this session
        self._last_ping_mono = 0.0
        self.reconnect_count = 0
        self.last_error: Optional[str] = None
        self.loaded_from_backup = False
        self._backup_dirty = threading.Event()
        self._backup_thread: Optional[threading.Thread] = None
        self._backup_write_lock = threading.Lock()
        self._keepalive_thread: Optional[threading.Thread] = None

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
        log.debug(msg)

    def _warn(self, msg: str) -> None:
        if not self.quiet:
            print(msg, flush=True)
        log.warning(msg)

    def _headers(self) -> list[str]:
        return [
            f"sdk-id-token: {self.kiponos_id}",
            f"sdk-access-token: {self.kiponos_access}",
            f"kiponos-id: {self.profile}",
            f"sdk-version: {SDK_VERSION}",
        ]

    # --- state ---

    @property
    def state(self) -> ConnectionState:
        return self._state

    def on_state_change(self, callback: StateHandler) -> StateHandler:
        """Register ``callback(old_state, new_state)``. Called from SDK threads;
        keep it fast. Returns the callback."""
        self._state_handlers.append(callback)
        return callback

    def _set_state(self, new: ConnectionState) -> None:
        with self._state_cond:
            old = self._state
            if old == new:
                return
            if old == ConnectionState.CLOSED:
                return  # terminal
            self._state = new
            self.is_connected = new == ConnectionState.READY
            self._state_cond.notify_all()
        self._log(f"state {old.value} -> {new.value}")
        for cb in list(self._state_handlers):
            try:
                cb(old, new)
            except Exception as e:
                self._warn(f"on_state_change error: {e}")

    def wait_ready(self, timeout: Optional[float] = None) -> bool:
        """Block until READY (True) or timeout / close (False)."""
        deadline = None if timeout is None else time.monotonic() + timeout
        with self._state_cond:
            while self._state != ConnectionState.READY:
                if self._state == ConnectionState.CLOSED:
                    return False
                remaining = None if deadline is None else deadline - time.monotonic()
                if remaining is not None and remaining <= 0:
                    return False
                self._state_cond.wait(remaining if remaining is not None else 1.0)
            return True

    # --- staleness / fail-closed ---

    @property
    def last_sync_time(self) -> Optional[float]:
        """Epoch seconds of the last moment the local tree was known to match
        the server. ``time.time()`` while READY; frozen while OFFLINE; the
        backup's save time after an offline startup; ``None`` if never synced."""
        if self._state == ConnectionState.READY:
            return time.time()
        return self._synced_at

    @property
    def staleness_seconds(self) -> float:
        """0.0 while READY, else seconds since the last known sync
        (``math.inf`` if never synced).

        Caveat: a silently dead network path is only noticed through the ping
        keepalive, so in the worst case the client stays READY for about
        ``ping_interval + ping_timeout`` seconds after the path died.
        """
        if self._state == ConnectionState.READY:
            return 0.0
        if self._synced_at is None:
            return math.inf
        return max(0.0, time.time() - self._synced_at)

    def is_fresh(self, max_staleness: float = 0.0) -> bool:
        return self.staleness_seconds <= max_staleness

    def require_fresh(self, max_staleness: float = 0.0) -> None:
        """Raise :class:`StaleConfigError` unless the config is at most
        ``max_staleness`` seconds stale. ``0`` means "must be READY now"."""
        s = self.staleness_seconds
        if s > max_staleness:
            raise StaleConfigError(
                f"Kiponos config is stale: state={self._state.value}, "
                f"staleness={s:.1f}s > max_staleness={max_staleness}s"
            )

    # --- events ---

    def on_change(
        self, callback: ChangeHandler, *, folder: Optional[str] = None
    ) -> ChangeHandler:
        """Register ``callback(key, value, folders, source, delta)``.

        ``folder="a/b"`` limits the callback to keys inside that folder (or its
        sub-folders). Runs on the SDK listener thread: prefer running heavy work
        off it (queue/worker). Returns the callback.
        """
        if folder:
            prefix = tuple(parse_path(folder))
            inner = callback

            def scoped(key, value, folders=(), source="", delta=None):
                if tuple(folders[: len(prefix)]) != prefix:
                    return
                try:
                    inner(key, value, folders, source, delta)
                except TypeError:
                    inner(key, value)

            scoped.__wrapped__ = callback  # type: ignore[attr-defined]
            self._on_change.append(scoped)
        else:
            self._on_change.append(callback)
        return callback

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

    def get(
        self,
        path: str,
        default: Any = None,
        *,
        max_staleness: Optional[float] = None,
    ) -> Any:
        """Get value by ``folder/sub/key`` path (or root key).

        ``max_staleness`` (seconds, optional): return ``default`` instead of the
        cached value when the config is staler than that — the fail-closed read.
        ``max_staleness=0`` means "only trust a value while READY".
        """
        if max_staleness is not None and self.staleness_seconds > max_staleness:
            return default
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

    # --- typed getters ---

    _MISSING = object()

    def _typed(
        self,
        path: str,
        default: Any,
        parse: Callable[[str], Any],
        type_name: str,
        min_v: Any,
        max_v: Any,
        max_staleness: Optional[float],
    ) -> Any:
        raw = self.get(path, self._MISSING, max_staleness=max_staleness)
        if raw is self._MISSING or raw is None:
            return default
        if isinstance(raw, str) and raw.strip() == "":
            return default
        try:
            val = parse(raw)
        except (TypeError, ValueError):
            self._warn(f"kiponos: {path}={raw!r} is not a valid {type_name}; using default")
            return default
        if (min_v is not None and val < min_v) or (max_v is not None and val > max_v):
            self._warn(
                f"kiponos: {path}={raw!r} out of range [{min_v}, {max_v}]; using default"
            )
            return default
        return val

    def get_int(
        self,
        path: str,
        default: Any = None,
        *,
        min: Optional[int] = None,  # noqa: A002 - public API name
        max: Optional[int] = None,  # noqa: A002
        max_staleness: Optional[float] = None,
    ) -> Any:
        """Integer value, or ``default`` when missing, empty, unparsable,
        outside ``[min, max]`` or (with ``max_staleness``) stale."""

        def parse(s: Any) -> int:
            s = str(s).strip()
            try:
                return int(s)
            except ValueError:
                f = float(s)
                if not f.is_integer():
                    raise
                return int(f)

        return self._typed(path, default, parse, "int", min, max, max_staleness)

    def get_float(
        self,
        path: str,
        default: Any = None,
        *,
        min: Optional[float] = None,  # noqa: A002
        max: Optional[float] = None,  # noqa: A002
        max_staleness: Optional[float] = None,
    ) -> Any:
        """Float value (NaN rejected), same fallback rules as :meth:`get_int`."""

        def parse(s: Any) -> float:
            f = float(str(s).strip())
            if math.isnan(f):
                raise ValueError("nan")
            return f

        return self._typed(path, default, parse, "float", min, max, max_staleness)

    def get_bool(
        self,
        path: str,
        default: Any = None,
        *,
        max_staleness: Optional[float] = None,
    ) -> Any:
        """Bool from ``true/false/1/0/yes/no/on/off/y/n`` (case-insensitive),
        else ``default``. With ``max_staleness`` a stale config yields
        ``default``: pass the *safe* value as default to fail closed."""

        def parse(s: Any) -> bool:
            t = str(s).strip().lower()
            if t in _TRUE:
                return True
            if t in _FALSE:
                return False
            raise ValueError(s)

        return self._typed(path, default, parse, "bool", None, None, max_staleness)

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
        if self._state == ConnectionState.CLOSED:
            # re-open after close(): start over
            with self._state_cond:
                self._state = ConnectionState.CONNECTING
            self._closing.clear()
        try:
            session = self._open_session()
        except AuthError:
            self.is_connected = False
            raise
        except KiponosError as e:
            self.is_connected = False
            self.last_error = str(e)
            if not self.offline_startup:
                raise
            backup = self._load_backup()
            if backup is None:
                raise ConnectionError(
                    f"{e} (offline_startup: no usable backup at {self.backup_path})"
                ) from e
            self._install_backup(backup)
            self._warn(
                f"Kiponos unreachable at startup ({e}); serving backup "
                f"{self.backup_path} saved {time.time() - (self._synced_at or 0):.0f}s ago, "
                "reconnecting in background"
            )
            self._set_state(ConnectionState.OFFLINE)
            self._start_threads(start_offline=True)
            return self

        self._install_session(*session, resync=False)
        self._log(f"Connected team={self.team_id} profile={self.profile}")
        self._start_threads(start_offline=False)
        return self

    def _start_threads(self, *, start_offline: bool) -> None:
        if self.backup_path and not (self._backup_thread and self._backup_thread.is_alive()):
            self._backup_thread = threading.Thread(
                target=self._backup_worker, name="kiponos-backup", daemon=True
            )
            self._backup_thread.start()
        if self.ping_interval and not (
            self._keepalive_thread and self._keepalive_thread.is_alive()
        ):
            self._keepalive_thread = threading.Thread(
                target=self._keepalive, name="kiponos-keepalive", daemon=True
            )
            self._keepalive_thread.start()
        self.listener_thread = threading.Thread(
            target=self._run, args=(start_offline,), name="kiponos-listener", daemon=True
        )
        self.listener_thread.start()

    def _open_session(self):
        """Handshake + STOMP CONNECT + bootstrap + team subscriptions on a new
        socket. Returns ``(ws, tree, team_info, handlers)``; installs nothing.
        Same frames, same order as 0.2.0 / the Java SDK."""
        ws = websocket.WebSocket()
        try:
            try:
                ws.connect(
                    self.server_url, header=self._headers(), timeout=self.connect_timeout
                )
            except websocket.WebSocketBadStatusException as e:
                status = getattr(e, "status_code", None)
                if status in (401, 403):
                    raise AuthError(
                        f"WebSocket handshake rejected (HTTP {status}). "
                        "Check tokens match the profile (Connect screen)."
                    ) from e
                raise ConnectionError(
                    f"WebSocket handshake failed: {e}. "
                    "Check tokens match the profile (Connect screen)."
                ) from e
            except Exception as e:
                raise ConnectionError(
                    f"WebSocket handshake failed: {e}. "
                    "Check tokens match the profile (Connect screen)."
                ) from e

            try:
                host = urlparse(self.server_url).hostname or "kiponos.io"
                ws.send(f"CONNECT\naccept-version:1.2\nhost:{host}\n\n\0")
                response = ws.recv()
            except Exception as e:
                raise ConnectionError(f"STOMP CONNECT failed: {e}") from e
            if isinstance(response, str) and response.startswith("ERROR"):
                raise AuthError(f"STOMP CONNECT rejected: {response!r}")
            if not isinstance(response, str) or not response.startswith("CONNECTED"):
                raise ConnectionError(f"STOMP CONNECT failed: {response!r}")

            try:
                ws.send(f"SUBSCRIBE\nid:0\ndestination:{self.Q_BOOTSTRAP}\n\n\0")
                boot_msg = ws.recv()
            except Exception as e:
                raise ConnectionError(f"Bootstrap failed: {e}") from e
            if not isinstance(boot_msg, bytes):
                raise KiponosError(f"Expected binary bootstrap, got {type(boot_msg)}")

            command, _headers, body = self._parse_stomp_frame(boot_msg)
            if command != "MESSAGE":
                raise KiponosError(f"Expected MESSAGE frame, got {command}")

            try:
                decompressed = zlib.decompress(body, wbits=-zlib.MAX_WBITS)
                json_arr = json.loads(decompressed.decode("utf-8"))
            except (zlib.error, ValueError) as e:
                raise KiponosError(f"Bootstrap payload unreadable: {e}") from e
            tree = json_arr[0] if len(json_arr) > 0 else {}
            team_info = json_arr[1] if len(json_arr) > 1 else {}
            if not isinstance(tree, dict) or not isinstance(team_info, dict):
                raise KiponosError("Bootstrap payload has unexpected shape")
            team_id = team_info.get("teamId") or ""
            if not team_id:
                raise KiponosError("Bootstrap missing teamId")

            handlers: dict[str, Callable] = {}
            sub_id = 200
            try:
                for topic, handler in (
                    ("config-key-created", self._on_key_created),
                    ("config-val-updated", self._on_val_updated),
                    ("config-prop-saved", self._on_prop_saved),
                    ("config-folder-created", self._on_folder_created),
                    ("config-key-deleted", self._on_key_deleted),
                ):
                    sub_id += 1
                    dest = f"/topic/team/{team_id}/{topic}"
                    handlers[str(sub_id)] = handler
                    ws.send(f"SUBSCRIBE\nid:{sub_id}\ndestination:{dest}\n\n\0")
            except Exception as e:
                raise ConnectionError(f"Subscribe failed: {e}") from e
            time.sleep(0.35)
            # The handshake timeout must not become an idle timeout for the
            # listener's recv() (0.2.0 bug: listener died after 25 s idle).
            ws.settimeout(None)
            return ws, tree, team_info, handlers, sub_id
        except BaseException:
            self._quiet_close_ws(ws)
            raise

    def _install_session(
        self, ws, tree: dict, team_info: dict, handlers: dict, sub_id: int, *, resync: bool
    ) -> None:
        with self._lock:
            old_tree = self.config_tree
            self.config_tree = tree
            self.team_info = team_info
            self.team_id = team_info.get("teamId") or ""
            self.handlers = handlers
            self.last_sub_id = sub_id
            self.ws = ws
            self._mark_rx()
            self._liveness_verified = False
            self._last_ping_mono = time.monotonic()
            self._synced_at = time.time()
            self.loaded_from_backup = False
        self._set_state(ConnectionState.READY)
        self._schedule_backup()
        if resync:
            self._emit_resync(old_tree, tree)

    def _install_backup(self, backup: dict) -> None:
        with self._lock:
            self.config_tree = backup["config"]
            info = backup.get("team_info")
            self.team_info = info if isinstance(info, dict) else {}
            self.team_id = backup.get("team_id") or self.team_info.get("teamId") or ""
            self._synced_at = float(backup["saved_at"])
            self.loaded_from_backup = True

    @staticmethod
    def _flatten(node: Any, prefix: tuple = ()) -> dict:
        out: dict = {}
        if not isinstance(node, dict):
            return out
        for k, v in node.items():
            if isinstance(v, dict) and VALUE_KEY in v:
                out[(prefix, k)] = v.get(VALUE_KEY)
            elif isinstance(v, dict):
                out.update(Kiponos._flatten(v, prefix + (k,)))
        return out

    def _emit_resync(self, old_tree: dict, new_tree: dict) -> None:
        """Fire on_change for keys added or changed while we were offline.
        Keys deleted while offline are removed from the tree silently, like
        live ``config-key-deleted`` events (which never fired on_change)."""
        old = self._flatten(old_tree)
        new = self._flatten(new_tree)
        for (folders, key), value in new.items():
            if (folders, key) in old and old[(folders, key)] == value:
                continue
            delta = {
                "key": key,
                "value": value,
                "oldValue": old.get((folders, key)),
                "basePath": join_json_path(self.base_path, *folders),
                "resync": True,
            }
            self._emit(key, value, list(folders), "resync", delta)

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

    # --- listener / reconnect ---

    def _mark_rx(self) -> None:
        self._last_rx_mono = time.monotonic()
        self._last_rx_epoch = time.time()

    def _run(self, start_offline: bool) -> None:
        ws = None if start_offline else self.ws
        while True:
            if ws is not None:
                self._main_listener(ws)
                if self._closing.is_set():
                    break
                self._on_connection_lost(ws)
            if not self.auto_reconnect or self._closing.is_set():
                break
            ws = self._reconnect_loop()
            if ws is None:
                break

    def _main_listener(self, ws=None) -> None:
        ws = ws or self.ws
        while ws is not None and self.ws is ws and not self._closing.is_set():
            try:
                opcode, frame = ws.recv_data_frame(True)
            except Exception as e:
                if not self._closing.is_set():
                    self.last_error = f"listener: {type(e).__name__}: {e}"
                    self._log(f"listener error: {e}")
                return
            self._mark_rx()
            if opcode == websocket.ABNF.OPCODE_CLOSE:
                if not self._closing.is_set():
                    self.last_error = "server closed the WebSocket"
                return
            if opcode == websocket.ABNF.OPCODE_PONG:
                self._liveness_verified = True
                continue
            if opcode != websocket.ABNF.OPCODE_TEXT:
                continue
            try:
                msg = frame.data.decode("utf-8")
            except Exception:
                continue
            if not msg.startswith("MESSAGE"):
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
            self._schedule_backup()

    def _on_connection_lost(self, ws) -> None:
        with self._lock:
            if self._state == ConnectionState.READY:
                # Last moment we know we were in sync. If the server has been
                # answering pings, that is the last frame we got; otherwise the
                # moment the drop was detected.
                self._synced_at = (
                    self._last_rx_epoch if self._liveness_verified else time.time()
                )
            if self.ws is ws:
                self.ws = None
        self._quiet_close_ws(ws)
        self._fail_pending()
        self._set_state(ConnectionState.OFFLINE)
        self._warn(
            f"Kiponos connection lost ({self.last_error}); serving last known config"
            + (", reconnecting" if self.auto_reconnect else "")
        )

    def _reconnect_loop(self):
        attempt = 0
        while not self._closing.is_set():
            if self._state == ConnectionState.SAFE_MODE:
                delay = self.reconnect_max_delay
            else:
                delay = min(
                    self.reconnect_max_delay,
                    self.reconnect_min_delay * (2 ** min(attempt, 30)),
                )
            if self._closing.wait(random.uniform(delay / 2.0, delay)):
                return None
            try:
                session = self._open_session()
            except AuthError as e:
                self.last_error = str(e)
                self._set_state(ConnectionState.SAFE_MODE)
                attempt += 1
                continue
            except Exception as e:
                self.last_error = str(e)
                self._log(f"reconnect attempt {attempt + 1} failed: {e}")
                if self._state == ConnectionState.SAFE_MODE:
                    self._set_state(ConnectionState.OFFLINE)
                attempt += 1
                continue
            if self._closing.is_set():
                self._quiet_close_ws(session[0])
                return None
            self.reconnect_count += 1
            self._install_session(*session, resync=True)
            self._log(f"Reconnected team={self.team_id} after {attempt + 1} attempt(s)")
            return session[0]
        return None

    def _keepalive(self) -> None:
        step = max(0.05, min(1.0, (self.ping_interval or 1.0) / 4.0))
        while not self._closing.wait(step):
            ws = self.ws
            if ws is None or self._state != ConnectionState.READY or not self.ping_interval:
                continue
            now = time.monotonic()
            if (
                self._liveness_verified
                and now - self._last_rx_mono > self.ping_interval + self.ping_timeout
            ):
                self.last_error = (
                    f"no frame from server for {now - self._last_rx_mono:.0f}s "
                    "(ping keepalive)"
                )
                self._warn(f"Kiponos {self.last_error}; dropping connection")
                try:
                    ws.abort()
                except Exception:
                    pass
                continue
            if now - self._last_ping_mono >= self.ping_interval:
                self._last_ping_mono = now
                try:
                    ws.ping()
                except Exception:
                    pass  # the listener will notice the broken socket

    # --- last-known-good backup ---

    def _schedule_backup(self) -> None:
        if self.backup_path:
            self._backup_dirty.set()

    def _backup_worker(self) -> None:
        while True:
            self._backup_dirty.wait(1.0)
            if self._backup_dirty.is_set():
                self._backup_dirty.clear()
                self.flush_backup()
            if self._closing.is_set():
                break

    def flush_backup(self) -> bool:
        """Write the backup file now (normally done in the background).
        Returns True on success. No-op (False) without ``backup_path`` or
        before the first successful sync."""
        if not self.backup_path:
            return False
        with self._lock:
            if self._synced_at is None:
                return False
            saved_at = (
                time.time() if self._state == ConnectionState.READY else self._synced_at
            )
            payload = json.dumps(
                {
                    "format": BACKUP_FORMAT,
                    "format_version": BACKUP_FORMAT_VERSION,
                    "saved_at": saved_at,
                    "profile": self.profile,
                    "team_id": self.team_id,
                    "team_info": self.team_info,
                    "config": self.config_tree,
                },
                separators=(",", ":"),
            )
        path = os.path.abspath(self.backup_path)
        tmp = f"{path}.tmp-{os.getpid()}-{threading.get_ident()}"
        with self._backup_write_lock:
            try:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
                with os.fdopen(fd, "w", encoding="utf-8") as f:
                    f.write(payload)
                    f.flush()
                    os.fsync(f.fileno())
                os.replace(tmp, path)  # atomic: readers see old or new, never half
                return True
            except OSError as e:
                self._warn(f"Kiponos backup write failed ({path}): {e}")
                try:
                    os.unlink(tmp)
                except OSError:
                    pass
                return False

    def _load_backup(self) -> Optional[dict]:
        path = self.backup_path
        if not path:
            return None
        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except FileNotFoundError:
            return None
        except (OSError, ValueError) as e:
            self._warn(f"Kiponos backup {path} unreadable, ignoring it: {e}")
            return None
        if (
            not isinstance(data, dict)
            or data.get("format") != BACKUP_FORMAT
            or not isinstance(data.get("config"), dict)
            or not isinstance(data.get("saved_at"), (int, float))
        ):
            self._warn(f"Kiponos backup {path} is not a valid backup, ignoring it")
            return None
        if data.get("profile") != self.profile:
            self._warn(
                f"Kiponos backup {path} is for profile {data.get('profile')!r}, "
                f"not {self.profile!r}; ignoring it"
            )
            return None
        return data

    # --- server push handlers ---

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

    # --- requests ---

    _LOST = object()

    def _fail_pending(self) -> None:
        for req_id, ev in list(self._pending.items()):
            self._pending_result[req_id] = self._LOST
            ev.set()

    def _new_request_id(self) -> str:
        return f"{int(time.time() * 1000)}-{uuid.uuid4().hex[:8]}"

    def _send_json(self, destination: str, payload: dict) -> None:
        ws = self.ws
        if not ws or not self.is_connected:
            raise ConnectionError(f"Not connected (state={self._state.value})")
        body = json.dumps(payload, separators=(",", ":"))
        frame = (
            f"SEND\n"
            f"destination:{destination}\n"
            f"content-type:application/json\n"
            f"content-length:{len(body.encode('utf-8'))}\n"
            f"\n"
            f"{body}\0"
        )
        try:
            ws.send(frame)
        except Exception as e:
            raise ConnectionError(f"Send failed: {e}") from e

    def _wait(self, request_id: str, timeout: Optional[float] = None) -> Any:
        ev = threading.Event()
        self._pending[request_id] = ev
        try:
            ok = ev.wait(timeout if timeout is not None else self.request_timeout)
            if not ok:
                raise TimeoutError(
                    f"No server response for request {request_id} within timeout"
                )
            result = self._pending_result.get(request_id)
            if result is self._LOST:
                raise ConnectionError(
                    f"Connection lost before the server confirmed request {request_id}"
                )
            return result
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
        """Disconnect and stop background threads. Safe to call more than once
        and from inside an ``on_change`` / ``on_state_change`` callback."""
        was_ready = self._state == ConnectionState.READY
        self._closing.set()
        with self._lock:
            if was_ready:
                self._synced_at = time.time()
            ws = self.ws
            self.ws = None
        self._set_state(ConnectionState.CLOSED)
        self.is_connected = False
        self._fail_pending()
        if ws is not None:
            try:
                ws.send("DISCONNECT\n\n\0")
            except Exception:
                pass
            self._quiet_close_ws(ws)
        current = threading.current_thread()
        for t in (self.listener_thread, self._keepalive_thread, self._backup_thread):
            # 0.2.0 joined the listener unconditionally, which raised
            # RuntimeError("cannot join current thread") from a callback.
            if t is not None and t.is_alive() and t is not current:
                t.join(timeout=3)
        if self.backup_path and self._backup_dirty.is_set():
            self._backup_dirty.clear()
            self.flush_backup()

    @staticmethod
    def _quiet_close_ws(ws) -> None:
        if ws is None:
            return
        try:
            ws.send_close()
        except Exception:
            pass
        try:
            ws.abort()  # unblocks a recv() in another thread
        except Exception:
            pass
        try:
            ws.shutdown()
        except Exception:
            pass

    def __enter__(self) -> "Kiponos":
        if self._state in (ConnectionState.CONNECTING, ConnectionState.CLOSED):
            self.open()
        return self

    def __exit__(self, *args) -> None:
        self.close()
