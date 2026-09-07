from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "code-mode"
    assert DEFAULT == "off"
    assert PATH.endswith("/code-mode")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "code_mode_on"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('yes')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('no')
    assert d['proceed'] is False
    assert d['action'] == 'schema_dump_mode'
