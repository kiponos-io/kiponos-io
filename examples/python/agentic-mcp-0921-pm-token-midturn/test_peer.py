from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "max-tokens"
    assert DEFAULT == "8000"
    assert PATH.endswith("/max-tokens")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "within_token_budget"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('8000')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('0')
    assert d['proceed'] is False
    assert d['action'] == 'stop_turn_budget'
