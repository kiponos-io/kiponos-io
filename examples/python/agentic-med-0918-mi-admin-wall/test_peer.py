from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "wall-focus"
    assert DEFAULT == "checkout"
    assert PATH.endswith("/wall-focus")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "admin_wall_focus"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('checkout')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('idle')
    assert d['proceed'] is False
    assert d['action'] == 'admin_wall_idle'
