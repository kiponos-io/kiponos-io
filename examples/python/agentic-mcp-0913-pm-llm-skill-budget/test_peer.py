from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "skill-budget"
    assert DEFAULT == "on"
    assert PATH.endswith("/skill-budget")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "generated_skills_ok"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('off')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('on')
    assert d['proceed'] is False
    assert d['action'] == 'generated_skills_blocked'
