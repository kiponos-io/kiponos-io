from chain_live_fraud import evaluate, Payment

def test_pass():
    d = evaluate("amount-cap,geo,velocity", 100000, "KP", 5, Payment(1000, "US", 1))
    assert d.allowed

def test_geo_blocks():
    d = evaluate("geo", 100000, "KP,IR", 5, Payment(100, "KP", 0))
    assert not d.allowed and "blocked" in d.reason
