from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "incident-pause"
    assert DEFAULT == "off"
    assert PATH.endswith("/incident-pause")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "shopping_path_live"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('off')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('on')
    assert d['proceed'] is False
    assert d['action'] == 'freeze_shopping_writes'
