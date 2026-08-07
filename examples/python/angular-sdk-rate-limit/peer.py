#!/usr/bin/env python3
"""Live Rate Limits the Angular Admin Proxy Honors — Python peer.

Pure helpers are unit-testable without tokens. Optional live hub via agent-kit.
Hub: limits/rps-cap (default 25)
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

FOLDER = "limits"
KEY = "rps-cap"
DEFAULT = "25"


def normalize(raw: str | None) -> str:
    v = (raw if raw is not None else DEFAULT).strip()
    return v or DEFAULT


def as_int(raw: str | None, default: int = 25) -> int:
    try:
        return int(normalize(raw if raw is not None else str(default)))
    except Exception:
        return default


def decide(value: str) -> dict:
    """Return posture decision for the peer hot path."""
    v = normalize(value)
    return {
        "key": KEY,
        "value": v,
        "human": "Angular admin proxy RPS cap",
        "action": "honor_live_leaf",
    }


def main() -> int:
    raw = sys.argv[1] if len(sys.argv) > 1 else DEFAULT
    d = decide(raw)
    print(f"{d['key']}={d['value']} action={d['action']}")
    # Optional live connect (do not restart process to flip leaf)
    if os.environ.get("KIPONOS_LIVE") == "1":
        try:
            repo = Path(__file__).resolve().parents[3]
            agent = repo / "agent-kit"
            if agent.is_dir() and str(agent) not in sys.path:
                sys.path.insert(0, str(agent))
            from kiponos import Kiponos  # type: ignore

            k = Kiponos.connect(quiet=True)
            # leaf path depends on kit API — moral is connect once, read live
            print("live peer online; flip hub leaf without killing this process")
            k.disconnect()
        except Exception as e:
            print("live optional skipped:", e)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
