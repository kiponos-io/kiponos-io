#!/usr/bin/env python3
"""Cursor Finished the Turn Blind — Mirror Phone live device leaf on the Mirror Phone Path

Python peer on the same Team tree as Java / React-Node / Angular-Node.
Hub: examples/agentic-dev-1028-pm-mirror-live/device-live (default yes)
Product: mirror-phone · Agent host: Cursor
Pain: Mirror Phone live device was a rumor in chat

  python3 peer.py                  # local decide()
  python3 -m pytest -q             # logic tests (no tokens)
  KIPONOS_LIVE=1 python3 peer.py   # live hub get/set
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

KEY = "device-live"
DEFAULT = "yes"
PATH = "examples/agentic-dev-1028-pm-mirror-live/device-live"
PEERS = ["java", "python", "react-node", "angular-node"]


def normalize(raw: str | None) -> str:
    v = (DEFAULT if raw is None else str(raw)).strip()
    return v or DEFAULT


def decide(value: str | None) -> dict:
    v = normalize(value)
    live = v.lower() in ("yes", "live", "on", "true")
    return {"path": PATH, "key": KEY, "value": v, "action": "mirror_device_live" if live else "route_other_mirror_device", "proceed": live, "peers": PEERS}


def _live(d: dict) -> None:
    repo = Path(__file__).resolve().parents[3]
    agent = repo / "agent-kit"
    if agent.is_dir() and str(agent) not in sys.path:
        sys.path.insert(0, str(agent))
    from kiponos import Kiponos  # type: ignore

    k = Kiponos.connect(quiet=True)
    try:
        k.ensure_path("examples/agentic-dev-1028-pm-mirror-live")
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
