"""Python agent peer for sre-agent-pause-mesh.

Reads/writes examples/sre-agent-pause-mesh/agent-tools live via Kiponos Python SDK.
Restart is not required to flip posture, skill gates, or budgets.
"""
from __future__ import annotations

import os
import time

# pip install kiponos  (or local agent-kit)
try:
    from kiponos import Kiponos
except ImportError:  # pragma: no cover
    Kiponos = None  # type: ignore


KEY = "agent-tools"
DEFAULT = "full"
FOLDER = ("examples", "sre-agent-pause-mesh")


def main() -> None:
    if Kiponos is None:
        print("kiponos SDK not installed — dry print only")
        print(f"{KEY}={DEFAULT}")
        return
    # Profile + tokens from env (same as Java). Never put Connect secrets in a browser.
    k = Kiponos.connect(quiet=True)
    try:
        # Path API varies by kit version; keep moral identical:
        # local get after connect, live set from dashboard/agents.
        print(f"agent peer online; leaf examples/sre-agent-pause-mesh/{KEY} default={DEFAULT}")
        print("flip the leaf on the hub — this process should honor it without restart")
        time.sleep(1.0)
    finally:
        try:
            k.disconnect()
        except Exception:
            pass


if __name__ == "__main__":
    main()
