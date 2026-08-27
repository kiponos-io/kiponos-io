from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "chat-mute"
    assert DEFAULT == "none"
    assert PATH.endswith("/chat-mute")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "group_chat_sends_live"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('none')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('ops-late-bags')
    assert d['proceed'] is False
    assert d['action'] == 'mute_sends_keep_session'
