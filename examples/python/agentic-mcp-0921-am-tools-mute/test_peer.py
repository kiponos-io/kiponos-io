from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "tools-mute"
    assert DEFAULT == "none"
    assert PATH.endswith("/tools-mute")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "tool_logs_live"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('none')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('ops-late-bags')
    assert d['proceed'] is False
    assert d['action'] == 'tool_logs_muted'
