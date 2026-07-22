#!/usr/bin/env python3
"""Super Pattern: Live Strategy Router (Python parity).

Logic is pure and unit-tested without tokens. Optional live hub via agent-kit.
"""
from __future__ import annotations

import os
import sys
from dataclasses import dataclass
from typing import Protocol

# Allow running from repo without install: prefer agent-kit on path
_REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
_AGENT = os.path.join(_REPO, "agent-kit")
if _AGENT not in sys.path:
    sys.path.insert(0, _AGENT)


@dataclass(frozen=True)
class StrategyContext:
    cart_cents: int
    loyalty_member: bool
    volume_threshold_cents: int
    loyalty_bps: int


class PricingStrategy(Protocol):
    def price_cents(self, ctx: StrategyContext) -> int: ...
    def describe(self, ctx: StrategyContext) -> str: ...


class FlatPricingStrategy:
    def price_cents(self, ctx: StrategyContext) -> int:
        return ctx.cart_cents

    def describe(self, ctx: StrategyContext) -> str:
        return "flat: no discount"


class VolumePricingStrategy:
    def price_cents(self, ctx: StrategyContext) -> int:
        if ctx.cart_cents >= ctx.volume_threshold_cents:
            return round(ctx.cart_cents * 0.95)
        return ctx.cart_cents

    def describe(self, ctx: StrategyContext) -> str:
        return f"volume: 5% off when cart >= {ctx.volume_threshold_cents} cents"


class LoyaltyPricingStrategy:
    def price_cents(self, ctx: StrategyContext) -> int:
        if not ctx.loyalty_member:
            return ctx.cart_cents
        bps = max(0, min(ctx.loyalty_bps, 9000))
        return round(ctx.cart_cents * (10_000 - bps) / 10_000)

    def describe(self, ctx: StrategyContext) -> str:
        return f"loyalty: {ctx.loyalty_bps} bps off for members"


STRATEGIES: dict[str, PricingStrategy] = {
    "flat": FlatPricingStrategy(),
    "volume": VolumePricingStrategy(),
    "loyalty": LoyaltyPricingStrategy(),
}


def price_cart(active: str, cart_cents: int, loyalty_member: bool,
               volume_threshold: int = 10_000, loyalty_bps: int = 150) -> tuple[str, int, str]:
    sid = (active or "flat").strip().lower()
    strategy = STRATEGIES.get(sid, STRATEGIES["flat"])
    ctx = StrategyContext(cart_cents, loyalty_member, volume_threshold, loyalty_bps)
    total = strategy.price_cents(ctx)
    return sid, total, strategy.describe(ctx)


def main() -> int:
    cart = int(sys.argv[1]) if len(sys.argv) > 1 else 12_500
    member = len(sys.argv) > 2 and sys.argv[2].lower() in {"yes", "true", "1", "on"}
    active = "flat"
    vol, bps = 10_000, 150
    try:
        from kiponos import Kiponos  # type: ignore
        with Kiponos.connect() as k:
            base = "patterns/strategy/checkout"
            if k.get(f"{base}/active") is None:
                k.set(f"{base}/active", "flat")
                k.set(f"{base}/volume-threshold", "10000")
                k.set(f"{base}/loyalty-bps", "150")
            active = str(k.get(f"{base}/active") or "flat")
            vol = int(k.get(f"{base}/volume-threshold") or 10000)
            bps = int(k.get(f"{base}/loyalty-bps") or 150)
            print("(live hub) loaded policy from Kiponos")
    except Exception as ex:
        print(f"(offline demo) using defaults: {ex.__class__.__name__}")
    sid, total, note = price_cart(active, cart, member, vol, bps)
    print("========================================")
    print("  Kiponos Super Pattern: Live Strategy (Python)")
    print(f"  active: {sid}")
    print(f"  cart:   {cart} cents")
    print(f"  total:  {total} cents")
    print(f"  note:   {note}")
    print("========================================")
    print("Full example (Java + tests): examples/java/pattern-strategy-live-router")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
