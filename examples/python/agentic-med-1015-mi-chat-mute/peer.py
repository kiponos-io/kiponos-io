#!/usr/bin/env python3
"""I Restarted Grok Build to Flip Group-chat mute without host kill — The Senses Wall Already Knew

Python peer on the same Team tree as Java / React-Node / Angular-Node.
Hub: examples/agentic-med-1015-mi-chat-mute/chat-mute (default none)
Product: senses · Agent host: Grok Build
Pain: Killing the agent host to stop a flooded travel group chat

  python3 peer.py                  # local decide()
  python3 -m pytest -q             # logic tests (no tokens)
  KIPONOS_LIVE=1 python3 peer.py   # live hub get/set
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

KEY = "chat-mute"
DEFAULT = "none"
PATH = "examples/agentic-med-1015-mi-chat-mute/chat-mute"
PEERS = ["java", "python", "react-node", "angular-node"]


def normalize(raw: str | None) -> str:
    v = (DEFAULT if raw is None else str(raw)).strip()
    return v or DEFAULT


def decide(value: str | None) -> dict:
    v = normalize(value)
    muted = v.lower() not in ("none", "off", "")
    return {"path": PATH, "key": KEY, "value": v, "action": "mute_sends_keep_session" if muted else "group_chat_sends_live", "proceed": not muted, "peers": PEERS}


def _live(d: dict) -> None:
    repo = Path(__file__).resolve().parents[3]
    agent = repo / "agent-kit"
    if agent.is_dir() and str(agent) not in sys.path:
        sys.path.insert(0, str(agent))
    from kiponos import Kiponos  # type: ignore

    k = Kiponos.connect(quiet=True)
    try:
        k.ensure_path("examples/agentic-med-1015-mi-chat-mute")
        live = k.get(PATH, DEFAULT)
        got = decide(str(live) if live is not None else DEFAULT)
        print("live", PATH, "=", got["value"], "action=", got["action"])
    finally:
        k.disconnect()


def main(argv: list[str] | None = None) -> int:
    argv = list(sys.argv[1:] if argv is None else argv)
    raw = argv[0] if argv else None
    d = decide(raw)
    print(f"{d['path']} => {d['value']} action={d['action']} proceed={d['proceed']}")
    print("peers:", ",".join(d["peers"]))
    if os.environ.get("KIPONOS_LIVE") == "1":
        try:
            _live(d)
        except Exception as e:
            print("live optional skipped:", e)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
