from peer import KEY, DEFAULT, decide, PATH_LABEL
def test_key():
    assert KEY == "canary-percent"
    assert "canary-percent" in PATH_LABEL
def test_decide():
    d = decide(None)
    assert d["value"] == "5"
    assert "java" in d["peers"] and "react-node" in d["peers"]
