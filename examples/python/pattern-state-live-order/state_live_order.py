#!/usr/bin/env python3
"""Super Pattern: Live State Machine — Python parity."""
from __future__ import annotations
import os, sys
from dataclasses import dataclass

_REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
sys.path.insert(0, os.path.join(_REPO, "agent-kit"))

@dataclass(frozen=True)
class TransitionResult:
    frm: str
    to: str
    ok: bool
    detail: str

def parse_allowed(raw: str) -> set[str]:
    out = set()
    for part in (raw or "").split(","):
        p = part.strip().lower().replace(" ", "")
        if ">" in p:
            out.add(p)
    return out

def try_transition(current: str, allowed_csv: str, next_state: str) -> TransitionResult:
    frm = (current or "draft").strip().lower()
    to = (next_state or "").strip().lower()
    edges = parse_allowed(allowed_csv)
    if frm == to:
        return TransitionResult(frm, to, True, f"already in {to}")
    edge = f"{frm}>{to}"
    if edge not in edges:
        return TransitionResult(frm, to, False, f"transition not allowed: {edge}")
    return TransitionResult(frm, to, True, f"transitioned {edge}")

def main() -> int:
    want = sys.argv[1] if len(sys.argv) > 1 else "paid"
    current, allowed = "draft", "draft>paid,paid>shipped,paid>cancelled,draft>cancelled"
    live = False
    try:
        from kiponos import Kiponos
        with Kiponos.connect() as k:
            base = "patterns/state/order"
            if k.get(f"{base}/current") is None:
                k.set(f"{base}/current", current)
                k.set(f"{base}/allowed", allowed)
            current = str(k.get(f"{base}/current") or current)
            allowed = str(k.get(f"{base}/allowed") or allowed)
            r = try_transition(current, allowed, want)
            if r.ok and r.frm != r.to:
                k.set(f"{base}/current", r.to)
            live = True
            print("(live hub) state policy loaded")
    except Exception as ex:
        print(f"(offline demo) {ex.__class__.__name__}")
        r = try_transition(current, allowed, want)
    print("from:", r.frm, "to:", r.to, "ok:", r.ok, "—", r.detail)
    print("Java twin: examples/java/pattern-state-live-order")
    return 0 if r.ok or not live else 0

if __name__ == "__main__":
    raise SystemExit(main())
