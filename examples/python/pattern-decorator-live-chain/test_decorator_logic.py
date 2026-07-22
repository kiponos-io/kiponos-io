from decorator_live_chain import execute, parse_chain

def test_parse():
    assert parse_chain(" metrics, Retry ") == ["metrics", "retry"]

def test_execute():
    chain, body, trace = execute("metrics,retry", "/x")
    assert "OK" in body
    assert any(t.startswith("core:") for t in trace)
