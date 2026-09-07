from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "owner-agent"
    assert DEFAULT == "travel-coordinator"
    assert PATH.endswith("/owner-agent")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "honor_chosen_owner"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('travel-coordinator')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('travel-coordinator')
    assert d['proceed'] is True
