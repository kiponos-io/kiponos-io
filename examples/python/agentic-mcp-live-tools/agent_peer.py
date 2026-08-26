"""Python agent peer for agentic-mcp-live-tools — live hub leaf, no MCP restart."""
from __future__ import annotations
import time
try:
    from kiponos import Kiponos
except ImportError:
    Kiponos = None  # type: ignore

FOLDER = "examples/agentic-mcp-live-tools"

def main() -> None:
    if Kiponos is None:
        print("kiponos SDK not installed — dry print only")
        print(FOLDER)
        return
    k = Kiponos.connect(quiet=True)
    try:
        print("agent peer online;", FOLDER, "— flip the leaf on the hub without restart")
        time.sleep(1.0)
    finally:
        try:
            k.disconnect()
        except Exception:
            pass

if __name__ == "__main__":
    main()
