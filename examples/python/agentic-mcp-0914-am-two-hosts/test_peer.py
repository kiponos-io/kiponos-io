from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "session-posture"
    assert DEFAULT == "focus=admin-wall,shopping-pause=off"
    assert PATH.endswith("/session-posture")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "share_session_posture"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('focus=admin-wall,shopping-pause=off')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('focus=admin-wall,shopping-pause=on')
    assert d['proceed'] is False
    assert d['action'] == 'incident_pause_active'
