from peer import KEY, DEFAULT, decide, normalize


def test_key_stable():
    assert KEY == "rps-cap"


def test_default():
    assert normalize(None) == "40"
    assert decide(None)["value"] == "40"


def test_override():
    d = decide("live-test-value")
    assert d["value"] == "live-test-value"
    assert d["action"] == "honor_live_leaf"
