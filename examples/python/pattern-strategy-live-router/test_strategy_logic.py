from strategy_live_router import price_cart, VolumePricingStrategy, StrategyContext

def test_flat():
    sid, total, _ = price_cart("flat", 12500, False)
    assert sid == "flat" and total == 12500

def test_volume():
    v = VolumePricingStrategy()
    assert v.price_cents(StrategyContext(9000, False, 10000, 150)) == 9000
    assert v.price_cents(StrategyContext(12500, False, 10000, 150)) == 11875

def test_loyalty():
    _, total, _ = price_cart("loyalty", 10000, True, loyalty_bps=150)
    assert total == 9850
