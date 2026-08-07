from peer import KEY, DEFAULT, decide, normalize


def test_key_stable():
    assert KEY == "pause-risky"


def test_default():
    assert normalize(None) == "off"
    assert decide(None)["value"] == "off"


def test_override():
    d = decide("live-test-value")
    assert d["value"] == "live-test-value"
    assert d["action"] == "honor_live_leaf"
