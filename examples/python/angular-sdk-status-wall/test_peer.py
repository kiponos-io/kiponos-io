from peer import KEY, DEFAULT, decide, normalize


def test_key_stable():
    assert KEY == "status-headline"


def test_default():
    assert normalize(None) == "steady"
    assert decide(None)["value"] == "steady"


def test_override():
    d = decide("live-test-value")
    assert d["value"] == "live-test-value"
    assert d["action"] == "honor_live_leaf"
