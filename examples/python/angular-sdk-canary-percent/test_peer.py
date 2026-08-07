from peer import KEY, DEFAULT, decide, normalize


def test_key_stable():
    assert KEY == "canary-percent"


def test_default():
    assert normalize(None) == "5"
    assert decide(None)["value"] == "5"


def test_override():
    d = decide("live-test-value")
    assert d["value"] == "live-test-value"
    assert d["action"] == "honor_live_leaf"
