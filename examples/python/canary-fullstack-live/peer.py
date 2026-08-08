#!/usr/bin/env python3
"""Canary Percent That Moves BFF, Workers, and UI Mirrors Together — Python peer on the same Team tree.

Hub leaf: release/canary-percent (default 5)
Connect once. Flip the leaf. Do not restart the session.
"""
from __future__ import annotations
import os, sys
from pathlib import Path

KEY = "canary-percent"
DEFAULT = "5"
PATH_LABEL = "release/canary-percent"


def normalize(raw: str | None) -> str:
    v = (raw if raw is not None else DEFAULT).strip()
    return v or DEFAULT


def decide(value: str | None) -> dict:
    v = normalize(value)
    return {
        "path": PATH_LABEL,
        "key": KEY,
        "value": v,
        "action": "honor_same_team_leaf",
        "peers": ["java", "python", "react-node", "angular-node"],
    }


def main() -> int:
    raw = sys.argv[1] if len(sys.argv) > 1 else DEFAULT
    d = decide(raw)
    print(f"{d['path']} => {d['value']} peers={','.join(d['peers'])}")
    if os.environ.get("KIPONOS_LIVE") == "1":
        try:
            repo = Path(__file__).resolve().parents[3]
            agent = repo / "agent-kit"
            if agent.is_dir() and str(agent) not in sys.path:
                sys.path.insert(0, str(agent))
            from kiponos import Kiponos  # type: ignore
            k = Kiponos.connect(quiet=True)
            print("live python peer online — same Team profile as Java/React/Angular")
            k.disconnect()
        except Exception as e:
            print("live optional skipped:", e)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
