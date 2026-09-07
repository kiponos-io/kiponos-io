from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "retry-max"
    assert DEFAULT == "2"
    assert PATH.endswith("/retry-max")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "retry_within_cap"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('8000')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('0')
    assert d['proceed'] is False
    assert d['action'] == 'doom_loop_stopped'
