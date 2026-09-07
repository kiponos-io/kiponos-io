from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "inherit-posture"
    assert DEFAULT == "on"
    assert PATH.endswith("/inherit-posture")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "child_reads_hub"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('yes')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('no')
    assert d['proceed'] is False
    assert d['action'] == 'child_frozen_argv'
