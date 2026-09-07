from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "shared-truth"
    assert DEFAULT == "live"
    assert PATH.endswith("/shared-truth")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "peers_share_live_hub"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('live')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('stale')
    assert d['proceed'] is False
    assert d['action'] == 'refuse_stale_host_argv'
