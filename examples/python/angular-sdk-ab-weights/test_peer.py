from peer import KEY, DEFAULT, decide, normalize


def test_key_stable():
    assert KEY == "ab-weights"


def test_default():
    assert normalize(None) == "70,30"
    assert decide(None)["value"] == "70,30"


def test_override():
    d = decide("live-test-value")
    assert d["value"] == "live-test-value"
    assert d["action"] == "honor_live_leaf"
