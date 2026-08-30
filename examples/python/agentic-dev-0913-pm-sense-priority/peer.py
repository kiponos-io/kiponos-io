#!/usr/bin/env python3
"""Grok Build Finished the Turn Blind — Sense priority mid-turn on the Mirror Phone Path

Python peer on the same Team tree as Java / React-Node / Angular-Node.
Hub: examples/agentic-dev-0913-pm-sense-priority/priority (default P3)
Product: mirror-phone · Agent host: Grok Build
Pain: Agent finishes a turn on stale sense truth because the probe lived in a file

  python3 peer.py                  # local decide()
  python3 -m pytest -q             # logic tests (no tokens)
  KIPONOS_LIVE=1 python3 peer.py   # live hub get/set
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

KEY = "priority"
DEFAULT = "P3"
PATH = "examples/agentic-dev-0913-pm-sense-priority/priority"
PEERS = ["java", "python", "react-node", "angular-node"]


def normalize(raw: str | None) -> str:
    v = (DEFAULT if raw is None else str(raw)).strip()
    return v or DEFAULT


def decide(value: str | None) -> dict:
    v = normalize(value)
    u = v.upper()
    abort = u.startswith("P0") or u.startswith("P1")
    return {"path": PATH, "key": KEY, "value": v, "action": "abort_mid_turn_no_restart" if abort else "continue_turn", "proceed": not abort, "peers": PEERS}


def _live(d: dict) -> None:
    repo = Path(__file__).resolve().parents[3]
    agent = repo / "agent-kit"
    if agent.is_dir() and str(agent) not in sys.path:
        sys.path.insert(0, str(agent))
    from kiponos import Kiponos  # type: ignore

    k = Kiponos.connect(quiet=True)
    try:
        k.ensure_path("examples/agentic-dev-0913-pm-sense-priority")

        def _on_change(key, value, folders=(), source="", delta=None):
            # dashboard edit → in-memory tree; MCP host does not restart
            print("on_change", "/".join(folders + (key,)), "=", value)

        if hasattr(k, "on_change"):
            k.on_change(_on_change)
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
