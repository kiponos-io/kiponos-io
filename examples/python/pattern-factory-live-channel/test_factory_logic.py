from factory_live_channel import create_notifier

def test_email():
    n = create_notifier("email", "a@b.c")
    assert n.channel() == "email"
    assert "a@b.c" in n.send("x")

def test_slack():
    n = create_notifier("slack", slack_hook="#x")
    assert "#x" in n.send("hi")
