from state_live_order import try_transition, parse_allowed

def test_parse():
    assert "draft>paid" in parse_allowed("draft>paid, paid > shipped")

def test_allowed():
    r = try_transition("draft", "draft>paid", "paid")
    assert r.ok

def test_blocked():
    r = try_transition("draft", "draft>paid", "shipped")
    assert not r.ok
