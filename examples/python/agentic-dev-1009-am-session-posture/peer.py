#!/usr/bin/env python3
"""MCP host Finished the Turn Blind — Session posture shared across hosts on the Shopping Path

Python peer on the same Team tree as Java / React-Node / Angular-Node.
Hub: examples/agentic-dev-1009-am-session-posture/session-posture (default focus=admin-wall,shopping-pause=off)
Product: shopping · Agent host: MCP host
Pain: Second agent host needed a paste buffer to see the same war-room posture

  python3 peer.py                  # local decide()
  python3 -m pytest -q             # logic tests (no tokens)
  KIPONOS_LIVE=1 python3 peer.py   # live hub get/set
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

KEY = "session-posture"
DEFAULT = "focus=admin-wall,shopping-pause=off"
PATH = "examples/agentic-dev-1009-am-session-posture/session-posture"
PEERS = ["java", "python", "react-node", "angular-node"]


def normalize(raw: str | None) -> str:
    v = (DEFAULT if raw is None else str(raw)).strip()
    return v or DEFAULT


def decide(value: str | None) -> dict:
    v = normalize(value)
    paused = "shopping-pause=on" in v.lower() or "pause=on" in v.lower()
    return {"path": PATH, "key": KEY, "value": v, "action": "incident_pause_active" if paused else "share_session_posture", "proceed": not paused, "peers": PEERS}


def _live(d: dict) -> None:
    repo = Path(__file__).resolve().parents[3]
    agent = repo / "agent-kit"
    if agent.is_dir() and str(agent) not in sys.path:
        sys.path.insert(0, str(agent))
    from kiponos import Kiponos  # type: ignore

    k = Kiponos.connect(quiet=True)
    try:
        k.ensure_path("examples/agentic-dev-1009-am-session-posture")
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
