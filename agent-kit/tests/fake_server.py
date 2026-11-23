"""Local stand-in for the Kiponos SDK WebSocket endpoint — tests only.

Speaks the subset of STOMP-over-WebSocket the Python SDK uses, as read from the
SDK source (and matching the Java sdk-boot-3 order): CONNECT -> CONNECTED,
SUBSCRIBE /user/queue/sdk-boot -> binary MESSAGE with raw-deflate JSON
``[tree, teamInfo]``, then team-topic subscriptions that deltas are pushed to.
This is NOT the real kiponos.io server; behaviour of the real server
(heartbeats, ping/pong, error codes) may differ.
"""
from __future__ import annotations

import asyncio
import json
import threading
import zlib

from aiohttp import WSMsgType, web

PROFILE = "['demo-app']['v1']['dev']['base']"
BASE = "$.rootAccount['apps']['demo-app']['rels']['v1']['envs']['dev']['cfgs']['base']"


def _deflate_raw(data: bytes) -> bytes:
    c = zlib.compressobj(wbits=-zlib.MAX_WBITS)
    return c.compress(data) + c.flush()


def _hdrs(frame: str) -> dict:
    head = frame.split("\n\n", 1)[0]
    return dict(line.split(":", 1) for line in head.split("\n")[1:] if ":" in line)


class FakeKiponos:
    def __init__(self, tree: dict, team_id: str = "team-1"):
        self.tree = tree
        self.team_id = team_id
        self.loop: asyncio.AbstractEventLoop | None = None
        self.conns: list[dict] = []  # {"ws":..., "subs": {topic: sid}, "silent": bool}
        self.frames: list[str] = []  # every text frame received, in order
        self.boot_count = 0
        self.handshakes = 0
        self.port = 0
        self.http_status: int | None = None  # reply this instead of upgrading
        self.stomp_error = False
        self.pong = True  # answer WebSocket pings
        self.ack_writes = True
        self._ready = threading.Event()
        self._runner: web.AppRunner | None = None
        self._site: web.TCPSite | None = None

    # ------------------------------------------------------------ server
    async def _handler(self, request):
        self.handshakes += 1
        if self.http_status:
            return web.Response(status=self.http_status)
        ws = web.WebSocketResponse(autoping=False)
        await ws.prepare(request)
        conn = {"ws": ws, "subs": {}, "silent": False}
        self.conns.append(conn)
        async for msg in ws:
            if msg.type == WSMsgType.PING:
                if self.pong and not conn["silent"]:
                    await ws.pong(msg.data)
                continue
            if msg.type != WSMsgType.TEXT:
                continue
            if conn["silent"]:
                continue
            frame = msg.data
            self.frames.append(frame)
            cmd = frame.split("\n", 1)[0]
            if cmd == "CONNECT":
                if self.stomp_error:
                    await ws.send_str("ERROR\nmessage:Access denied\n\n\0")
                    await ws.close()
                    break
                await ws.send_str("CONNECTED\nversion:1.2\n\n\0")
            elif cmd == "SUBSCRIBE":
                h = _hdrs(frame)
                dest, sid = h["destination"], h["id"]
                if dest == "/user/queue/sdk-boot":
                    self.boot_count += 1
                    body = _deflate_raw(json.dumps([self.tree, {"teamId": self.team_id}]).encode())
                    await ws.send_bytes(b"MESSAGE\nsubscription:" + sid.encode() + b"\n\n" + body + b"\0")
                else:
                    conn["subs"][dest.rsplit("/", 1)[-1]] = sid
            elif cmd == "SEND":
                await self._on_send(frame)
            elif cmd == "DISCONNECT":
                await ws.close()
        if conn in self.conns:
            self.conns.remove(conn)
        return ws

    async def _on_send(self, frame: str):
        dest = _hdrs(frame).get("destination", "")
        body = json.loads(frame.split("\n\n", 1)[1].rstrip("\0"))
        if not self.ack_writes:
            return
        if dest == "/app/sdk-save-config-prop":
            node = self._node_for(body["basePath"])
            node[body["key"]] = {"value": body["value"]}
            await self._broadcast("config-prop-saved", body)
        elif dest == "/app/sdk-create-config-folder":
            node = self._node_for(body["path"])
            node.setdefault(body["folder"], {})
            await self._broadcast("config-folder-created", body)
        elif dest == "/app/delete-config-key":
            node = self._node_for(body["basePath"])
            node.pop(body["key"], None)
            await self._broadcast("config-key-deleted", body)

    def _node_for(self, base_path: str) -> dict:
        import re

        rel = base_path[len(BASE):] if base_path.startswith(BASE) else ""
        node = self.tree
        for f in re.findall(r"\['([^']*)'\]", rel):
            node = node.setdefault(f, {})
        return node

    async def _broadcast(self, topic: str, payload: dict):
        body = json.dumps(payload)
        for c in list(self.conns):
            sid = c["subs"].get(topic)
            if sid and not c["ws"].closed and not c["silent"]:
                await c["ws"].send_str(f"MESSAGE\nsubscription:{sid}\n\n{body}\0")

    # ------------------------------------------------------------ control
    def _run(self):
        self.loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self.loop)
        app = web.Application()
        app.router.add_get("/api/io-kiponos-sdk", self._handler)
        self._runner = web.AppRunner(app)
        self.loop.run_until_complete(self._runner.setup())
        self.loop.run_until_complete(self._start_site())
        self._ready.set()
        self.loop.run_forever()

    async def _start_site(self):
        self._site = web.TCPSite(self._runner, "127.0.0.1", self.port, reuse_address=True)
        await self._site.start()
        self.port = self._site._server.sockets[0].getsockname()[1]

    def _call(self, coro, timeout=10):
        return asyncio.run_coroutine_threadsafe(coro, self.loop).result(timeout)

    def start(self) -> "FakeKiponos":
        threading.Thread(target=self._run, daemon=True).start()
        assert self._ready.wait(5)
        return self

    @property
    def url(self) -> str:
        return f"ws://127.0.0.1:{self.port}/api/io-kiponos-sdk"

    def push_value(self, folders: list[str], key: str, value: str, *, update_tree=True) -> None:
        if update_tree:
            node = self.tree
            for f in folders:
                node = node.setdefault(f, {})
            node[key] = {"value": value}
        base = BASE + "".join(f"['{f}']" for f in folders)
        self._call(self._broadcast("config-val-updated", {"basePath": base, "key": key, "value": value}))

    def drop_connections(self) -> None:
        async def _close():
            for c in list(self.conns):
                await c["ws"].close()

        self._call(_close())

    def go_silent(self) -> None:
        """Existing connections stop answering anything (half-open link)."""
        for c in self.conns:
            c["silent"] = True

    def go_down(self) -> None:
        """Stop listening (connection refused) and drop live connections."""
        async def _down():
            await self._site.stop()
            for c in list(self.conns):
                await c["ws"].close()

        self._call(_down())

    def go_up(self) -> None:
        self._call(self._start_site())

    def stop(self) -> None:
        if self.loop:
            try:
                self.drop_connections()
                self._call(self._runner.cleanup(), 15)
            except Exception:
                pass
            self.loop.call_soon_threadsafe(self.loop.stop)
