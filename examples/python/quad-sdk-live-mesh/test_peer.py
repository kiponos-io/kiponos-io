from peer import KEY, DEFAULT, decide, PATH_LABEL
def test_key():
    assert KEY == "mode"
    assert "mode" in PATH_LABEL
def test_decide():
    d = decide(None)
    assert d["value"] == "steady"
    assert "java" in d["peers"] and "react-node" in d["peers"]
