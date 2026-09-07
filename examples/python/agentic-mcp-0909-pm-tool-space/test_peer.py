from peer import KEY, DEFAULT, PATH, decide

def test_leaf():
    assert KEY == "tools-allow"
    assert DEFAULT == "search,read"
    assert PATH.endswith("/tools-allow")

def test_default_proceeds():
    d = decide(None)
    assert d["value"] == DEFAULT
    assert d["proceed"] is True
    assert d["action"] == "allow_listed_tools"
    assert "java" in d["peers"] and "react-node" in d["peers"]

def test_ok_sample():
    d = decide('search,read')
    assert d["proceed"] is True

def test_gated_sample():
    d = decide('search,read,write')
    assert d['proceed'] is False
    assert d['action'] == 'deny_write_no_mcp_restart'
