#!/usr/bin/env python3
"""Super Pattern: Live Chain of Responsibility (fraud) — Python parity."""
from __future__ import annotations
import os, sys
from dataclasses import dataclass

_REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
sys.path.insert(0, os.path.join(_REPO, "agent-kit"))

@dataclass(frozen=True)
class Payment:
    amount_cents: int
    country: str
    recent_orders: int

@dataclass(frozen=True)
class Decision:
    allowed: bool
    reason: str
    trail: list[str]

def evaluate(order_csv: str, amount_cap: int, blocked: str, velocity_max: int, payment: Payment) -> Decision:
    order = [p.strip().lower() for p in order_csv.split(",") if p.strip()]
    blocked_set = {c.strip().upper() for c in blocked.split(",") if c.strip()}
    trail = []
    for hid in order:
        if hid == "amount-cap":
            if payment.amount_cents > amount_cap:
                trail.append("amount-cap:reject")
                return Decision(False, f"amount {payment.amount_cents} > cap {amount_cap}", trail)
            trail.append("amount-cap:ok")
        elif hid == "geo":
            if payment.country.upper() in blocked_set:
                trail.append("geo:reject")
                return Decision(False, f"blocked country {payment.country}", trail)
            trail.append("geo:ok")
        elif hid == "velocity":
            if payment.recent_orders > velocity_max:
                trail.append("velocity:reject")
                return Decision(False, f"recentOrders {payment.recent_orders} > {velocity_max}", trail)
            trail.append("velocity:ok")
        else:
            trail.append(f"{hid}:skip-unknown")
    return Decision(True, "all handlers passed", trail)

def main() -> int:
    amount = int(sys.argv[1]) if len(sys.argv) > 1 else 25000
    country = sys.argv[2] if len(sys.argv) > 2 else "US"
    recent = int(sys.argv[3]) if len(sys.argv) > 3 else 2
    order, cap, blocked, vel = "amount-cap,geo,velocity", 100_000, "KP,IR,SY", 5
    try:
        from kiponos import Kiponos
        with Kiponos.connect() as k:
            base = "patterns/chain/fraud"
            if k.get(f"{base}/order") is None:
                k.set(f"{base}/order", order)
                k.set(f"{base}/amount-cap-cents", str(cap))
                k.set(f"{base}/blocked-countries", blocked)
                k.set(f"{base}/velocity-max", str(vel))
            order = str(k.get(f"{base}/order") or order)
            cap = int(k.get(f"{base}/amount-cap-cents") or cap)
            blocked = str(k.get(f"{base}/blocked-countries") or blocked)
            vel = int(k.get(f"{base}/velocity-max") or vel)
            print("(live hub) loaded fraud chain policy")
    except Exception as ex:
        print(f"(offline demo) {ex.__class__.__name__}")
    d = evaluate(order, cap, blocked, vel, Payment(amount, country, recent))
    print("allowed:", d.allowed)
    print("reason:", d.reason)
    print("trail:", " → ".join(d.trail))
    print("Java twin: examples/java/pattern-chain-live-fraud")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
