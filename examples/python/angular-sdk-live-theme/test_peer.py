from peer import KEY, DEFAULT, decide, normalize


def test_key_stable():
    assert KEY == "theme"


def test_default():
    assert normalize(None) == "night"
    assert decide(None)["value"] == "night"


def test_override():
    d = decide("live-test-value")
    assert d["value"] == "live-test-value"
    assert d["action"] == "honor_live_leaf"
