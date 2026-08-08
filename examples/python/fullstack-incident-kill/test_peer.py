from peer import KEY, DEFAULT, decide, PATH_LABEL
def test_key():
    assert KEY == "path-enabled"
    assert "path-enabled" in PATH_LABEL
def test_decide():
    d = decide(None)
    assert d["value"] == "on"
    assert "java" in d["peers"] and "react-node" in d["peers"]
