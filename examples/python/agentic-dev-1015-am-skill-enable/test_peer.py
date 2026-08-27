from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "enabled-set"
    assert DEFAULT == "research,notify"
    assert PATH.endswith("/enabled-set")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "honor_enabled_skills"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('research,notify')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('research,notify')
    assert d['proceed'] is True
