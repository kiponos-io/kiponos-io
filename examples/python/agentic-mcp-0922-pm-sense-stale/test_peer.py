from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "priority"
    assert DEFAULT == "P3"
    assert PATH.endswith("/priority")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "continue_turn"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('P3')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('P1')
    assert d['proceed'] is False
    assert d['action'] == 'abort_mid_turn_no_restart'
