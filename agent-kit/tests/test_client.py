"""Kiponos Python SDK tests against a LOCAL FAKE server (tests/fake_server.py).

None of these talk to the real kiponos.io.
"""
from __future__ import annotations

import json
import math
import os
import stat
import threading
import time

import pytest

from fake_server import BASE, PROFILE, FakeKiponos
from kiponos import (
    AuthError,
    ConnectionError,
    ConnectionState,
    Kiponos,
    KiponosError,
    StaleConfigError,
)

os.environ.setdefault("KIPONOS_ID", "test-id")
os.environ.setdefault("KIPONOS_ACCESS", "test-access")

S = ConnectionState


def wait_for(pred, timeout=5.0, step=0.02):
    end = time.time() + timeout
    while time.time() < end:
        if pred():
            return True
        time.sleep(step)
    return False


def tree():
    return {
        "trading": {"halt": {"value": "false"}, "max-qty": {"value": "10"}},
        "greeting": {"value": "hello"},
        "voice": {"vad": {"silence": {"value": "0.7"}}},
    }


FAST = dict(reconnect_min_delay=0.05, reconnect_max_delay=0.3)


@pytest.fixture
def server():
    s = FakeKiponos(tree()).start()
    yield s
    s.stop()


def connect(server, **kw):
    return Kiponos.connect(profile=PROFILE, server_url=server.url, **kw)


# ------------------------------------------------------------------ old API
def test_backward_compatible_api(server):
    k = connect(server)
    try:
        assert k.is_connected is True
        assert k.state is S.READY
        assert k.team_id == "team-1"
        assert k.get("greeting") == "hello"
        assert k.get("trading/max-qty") == "10"
        assert k.get("trading.max-qty") == "10"
        assert k.get("voice/vad/silence") == "0.7"
        assert k.get("nope", "dflt") == "dflt"
        assert k.get("trading/nope") is None
        assert k.list_keys() == ["greeting"]
        assert k.list_folders() == ["trading", "voice"]
        assert k.list_keys("trading") == ["halt", "max-qty"]
        assert k.dump("trading") == {"halt": {"value": "false"}, "max-qty": {"value": "10"}}

        five, two = [], []
        k.on_change(lambda key, value, folders, source, delta: five.append((key, value, folders, source)))
        k.on_change(lambda key, value: two.append((key, value)))
        server.push_value(["trading"], "max-qty", "20")
        assert wait_for(lambda: five and two)
        assert five[-1] == ("max-qty", "20", ("trading",), "config-val-updated")
        assert two[-1] == ("max-qty", "20")
        assert k.get("trading/max-qty") == "20"

        assert k.set("trading/max-qty", "30") == "30"
        assert server.tree["trading"]["max-qty"] == {"value": "30"}
        k.mkdir("newdir")
        assert "newdir" in k.list_folders()
        k.delete_key("greeting")
        assert k.get("greeting") is None
    finally:
        k.close()
    assert k.is_connected is False
    assert k.state is S.CLOSED


def test_context_manager_and_reopen(server):
    with connect(server) as k:
        assert k.get("greeting") == "hello"
    assert not k.is_connected
    with Kiponos(profile=PROFILE, server_url=server.url) as k2:
        assert k2.is_connected
    k2.open()  # re-open after close works as in 0.2.0
    assert k2.is_connected
    k2.close()


def test_wire_frames_same_order_as_0_2_0(server):
    k = connect(server)
    k.close()
    cmds = [f.split("\n", 2)[:2] for f in server.frames]
    assert cmds[0][0] == "CONNECT"
    assert cmds[1] == ["SUBSCRIBE", "id:0"]
    topics = [f.split("destination:")[1].split("\n")[0] for f in server.frames[2:7]]
    assert topics == [
        "/topic/team/team-1/config-key-created",
        "/topic/team/team-1/config-val-updated",
        "/topic/team/team-1/config-prop-saved",
        "/topic/team/team-1/config-folder-created",
        "/topic/team/team-1/config-key-deleted",
    ]


# ------------------------------------------------------------------ idle timeout
@pytest.mark.slow
def test_idle_survives_past_old_25s_cutoff(server):
    # pings disabled so this proves the recv() itself no longer times out
    k = connect(server, ping_interval=None)
    try:
        seen = []
        k.on_change(lambda key, value, *a: seen.append(value))
        time.sleep(28)
        assert k.is_connected and k.state is S.READY
        assert k.listener_thread.is_alive()
        assert k.reconnect_count == 0
        server.push_value(["trading"], "halt", "true")
        assert wait_for(lambda: seen == ["true"])
    finally:
        k.close()


# ------------------------------------------------------------------ reconnect + resync
def test_drop_reconnect_resync_fires_on_change_for_missed(server):
    k = connect(server, **FAST)
    try:
        states, events = [], []
        k.on_state_change(lambda old, new: states.append((old, new)))
        k.on_change(lambda key, value, folders, source, delta: events.append((key, value, folders, source, delta)))
        # change on the server WITHOUT a push = a change missed while offline
        server.tree["trading"]["halt"] = {"value": "true"}
        server.tree["trading"]["new-key"] = {"value": "x"}
        del server.tree["greeting"]
        server.drop_connections()
        assert wait_for(lambda: k.reconnect_count == 1 and k.state is S.READY, 5)
        assert (S.READY, S.OFFLINE) in states and (S.OFFLINE, S.READY) in states
        assert server.boot_count == 2  # full bootstrap re-fetched
        assert k.get("trading/halt") == "true"
        assert k.get("trading/new-key") == "x"
        assert k.get("greeting") is None
        got = {(e[0], e[1], e[2], e[3]) for e in events}
        assert got == {
            ("halt", "true", ("trading",), "resync"),
            ("new-key", "x", ("trading",), "resync"),
        }
        halt = [e for e in events if e[0] == "halt"][0][4]
        assert halt["oldValue"] == "false" and halt["resync"] is True
        assert halt["basePath"] == BASE + "['trading']"
        # live pushes work on the new session
        server.push_value(["trading"], "max-qty", "11")
        assert wait_for(lambda: k.get("trading/max-qty") == "11")
    finally:
        k.close()


def test_server_down_then_up(server):
    k = connect(server, **FAST)
    try:
        server.go_down()
        assert wait_for(lambda: k.state is S.OFFLINE, 3)
        assert not k.is_connected
        assert k.get("greeting") == "hello"  # still served from memory
        with pytest.raises(ConnectionError):
            k.set("greeting", "x")  # writes are never queued
        time.sleep(1.0)
        assert k.state is S.OFFLINE and server.handshakes >= 1
        server.go_up()
        assert wait_for(lambda: k.state is S.READY, 5)
        assert k.set("greeting", "back") == "back"
    finally:
        k.close()


def test_auto_reconnect_false_keeps_old_behaviour(server):
    k = connect(server, auto_reconnect=False)
    try:
        server.drop_connections()
        assert wait_for(lambda: not k.is_connected, 3)
        time.sleep(0.5)
        assert k.state is S.OFFLINE and k.reconnect_count == 0
        assert server.boot_count == 1
        assert k.get("greeting") == "hello"
    finally:
        k.close()


def test_pending_set_fails_fast_on_drop(server):
    server.ack_writes = False
    k = connect(server, request_timeout=20, **FAST)
    try:
        err = []

        def writer():
            try:
                k.set("greeting", "x")
            except Exception as e:
                err.append(e)

        t = threading.Thread(target=writer)
        t.start()
        time.sleep(0.3)
        t0 = time.time()
        server.drop_connections()
        t.join(5)
        assert err and isinstance(err[0], ConnectionError)
        assert time.time() - t0 < 3
    finally:
        k.close()


def test_close_during_backoff_is_quick(server):
    k = connect(server, reconnect_min_delay=5, reconnect_max_delay=10)
    server.go_down()
    assert wait_for(lambda: k.state is S.OFFLINE, 3)
    t0 = time.time()
    k.close()
    assert time.time() - t0 < 2
    assert not k.listener_thread.is_alive()


# ------------------------------------------------------------------ keepalive
def test_keepalive_detects_half_open_link(server):
    k = connect(server, ping_interval=0.3, ping_timeout=0.3, **FAST)
    try:
        assert wait_for(lambda: k._liveness_verified, 3)
        server.go_silent()  # TCP stays open, nothing comes back
        t0 = time.time()
        assert wait_for(lambda: k.state is not S.READY, 5)
        assert time.time() - t0 < 2.5
        assert wait_for(lambda: k.state is S.READY and k.reconnect_count == 1, 5)
    finally:
        k.close()


def test_server_without_pong_does_not_flap(server):
    server.pong = False
    k = connect(server, ping_interval=0.2, ping_timeout=0.2)
    try:
        time.sleep(1.5)
        assert k.state is S.READY and k.reconnect_count == 0
        assert not k._liveness_verified
    finally:
        k.close()


# ------------------------------------------------------------------ backup / offline startup
def test_backup_written_atomically_and_private(server, tmp_path):
    p = tmp_path / "sub" / "lkg.json"
    k = connect(server, backup_path=str(p))
    try:
        assert wait_for(p.exists, 3)
        data = json.loads(p.read_text())
        assert data["format"] == "kiponos-lkg" and data["profile"] == PROFILE
        assert data["config"]["greeting"] == {"value": "hello"}
        assert stat.S_IMODE(p.stat().st_mode) == 0o600
        server.push_value(["trading"], "halt", "true")
        assert wait_for(lambda: json.loads(p.read_text())["config"]["trading"]["halt"]["value"] == "true", 3)
        assert [f.name for f in p.parent.iterdir()] == ["lkg.json"]  # no tmp leftovers
    finally:
        k.close()


def test_offline_startup_from_backup_then_recovers(server, tmp_path):
    p = tmp_path / "lkg.json"
    k = connect(server, backup_path=str(p))
    k.close()
    saved = json.loads(p.read_text())["saved_at"]
    server.go_down()
    server.tree["greeting"] = {"value": "changed-while-we-were-down"}

    k = connect(server, backup_path=str(p), offline_startup=True, **FAST)
    try:
        assert k.state is S.OFFLINE and k.loaded_from_backup
        assert not k.is_connected
        assert k.get("greeting") == "hello"
        assert k.last_sync_time == pytest.approx(saved)
        assert k.staleness_seconds >= 0
        assert k.get("greeting", "SAFE", max_staleness=0) == "SAFE"
        events = []
        k.on_change(lambda key, value, folders, source, delta: events.append((key, value, source)))
        server.go_up()
        assert wait_for(lambda: k.state is S.READY, 5)
        assert k.get("greeting") == "changed-while-we-were-down"
        assert ("greeting", "changed-while-we-were-down", "resync") in events
        assert not k.loaded_from_backup
    finally:
        k.close()


def test_startup_no_server_no_backup_raises(server, tmp_path):
    server.go_down()
    with pytest.raises(ConnectionError):
        connect(server, backup_path=str(tmp_path / "missing.json"), offline_startup=True)


def test_startup_no_server_with_backup_but_not_opted_in_raises(server, tmp_path):
    p = tmp_path / "lkg.json"
    connect(server, backup_path=str(p)).close()
    server.go_down()
    with pytest.raises(ConnectionError):
        connect(server, backup_path=str(p))  # offline_startup defaults to False


@pytest.mark.parametrize(
    "content",
    [
        "{not json",
        "",
        "[]",
        json.dumps({"format": "other", "saved_at": 1, "config": {}, "profile": PROFILE}),
        json.dumps({"format": "kiponos-lkg", "saved_at": "x", "config": {}, "profile": PROFILE}),
        json.dumps({"format": "kiponos-lkg", "saved_at": 1, "config": {}, "profile": "['x']['y']['z']['w']"}),
    ],
)
def test_corrupt_or_foreign_backup_is_ignored(server, tmp_path, content):
    p = tmp_path / "lkg.json"
    p.write_text(content)
    server.go_down()
    with pytest.raises(ConnectionError):
        connect(server, backup_path=str(p), offline_startup=True)


def test_offline_startup_requires_backup_path():
    with pytest.raises(KiponosError):
        Kiponos(profile=PROFILE, offline_startup=True)


def test_env_options(server, tmp_path, monkeypatch):
    p = tmp_path / "lkg.json"
    monkeypatch.setenv("KIPONOS_BACKUP_PATH", str(p))
    connect(server).close()
    assert p.exists()
    monkeypatch.setenv("KIPONOS_OFFLINE_STARTUP", "1")
    server.go_down()
    k = connect(server)
    try:
        assert k.state is S.OFFLINE and k.get("greeting") == "hello"
    finally:
        k.close()


# ------------------------------------------------------------------ auth
def test_http_401_raises_auth_error_even_with_backup(server, tmp_path):
    p = tmp_path / "lkg.json"
    connect(server, backup_path=str(p)).close()
    server.http_status = 401
    with pytest.raises(AuthError):
        connect(server, backup_path=str(p), offline_startup=True)


def test_http_500_is_a_connection_error_and_allows_backup(server, tmp_path):
    # NB: the real kiponos.io answered bogus tokens with HTTP 500 (probed
    # 2026-11-20), so a 500 can't be told apart from a server fault.
    p = tmp_path / "lkg.json"
    connect(server, backup_path=str(p)).close()
    server.http_status = 500
    with pytest.raises(ConnectionError) as ei:
        connect(server)
    assert not isinstance(ei.value, AuthError)
    k = connect(server, backup_path=str(p), offline_startup=True)
    assert k.state is S.OFFLINE
    k.close()


def test_stomp_error_is_auth_error(server):
    server.stomp_error = True
    with pytest.raises(AuthError):
        connect(server)


def test_safe_mode_on_reconnect_auth_rejection(server):
    k = connect(server, **FAST)
    try:
        server.http_status = 403
        server.drop_connections()
        assert wait_for(lambda: k.state is S.SAFE_MODE, 5)
        assert k.get("greeting") == "hello"
        server.http_status = None
        assert wait_for(lambda: k.state is S.READY, 5)
    finally:
        k.close()


# ------------------------------------------------------------------ staleness / fail-closed
def test_staleness_and_fail_closed(server):
    k = connect(server, **FAST)
    try:
        assert k.staleness_seconds == 0.0
        assert k.is_fresh() and k.last_sync_time == pytest.approx(time.time(), abs=1)
        k.require_fresh()
        assert k.get_bool("trading/halt", default=True, max_staleness=0) is False
        server.go_down()
        assert wait_for(lambda: k.state is S.OFFLINE, 3)
        time.sleep(0.3)
        s = k.staleness_seconds
        assert 0.2 < s < 5
        assert not k.is_fresh(0.1) and k.is_fresh(60)
        with pytest.raises(StaleConfigError):
            k.require_fresh(0.1)
        k.require_fresh(60)
        # kill switch fails closed: stale -> halted
        assert k.get_bool("trading/halt", default=True, max_staleness=0.1) is True
        assert k.get("trading/halt", "true", max_staleness=0.1) == "true"
        # a caller that tolerates staleness still gets the cached value
        assert k.get("trading/halt", max_staleness=60) == "false"
        assert k.get("trading/halt") == "false"  # 0.2.0 behaviour unchanged
        server.go_up()
        assert wait_for(lambda: k.state is S.READY, 5)
        assert k.staleness_seconds == 0.0
    finally:
        k.close()


def test_never_synced_is_infinitely_stale():
    k = Kiponos(profile=PROFILE)
    assert k.state is S.CONNECTING
    assert math.isinf(k.staleness_seconds) and k.last_sync_time is None
    assert k.get("x", "SAFE", max_staleness=1e9) == "SAFE"


# ------------------------------------------------------------------ typed getters / folder listener
def test_typed_getters(server):
    server.tree.update(
        {
            "n": {"i": {"value": "42"}, "f": {"value": "0.55"}, "bad": {"value": "abc"},
                  "empty": {"value": ""}, "fi": {"value": "3.0"}, "nan": {"value": "nan"},
                  "yes": {"value": " Yes "}, "off": {"value": "OFF"}},
        }
    )
    k = connect(server)
    try:
        assert k.get_int("n/i") == 42
        assert k.get_int("n/fi") == 3
        assert k.get_int("n/f", 7) == 7
        assert k.get_int("n/bad", 7) == 7
        assert k.get_int("n/empty", 7) == 7
        assert k.get_int("n/missing", 7) == 7
        assert k.get_int("n/i", 5, min=0, max=10) == 5  # out of range -> default
        assert k.get_int("n/i", 5, min=0, max=100) == 42
        assert k.get_float("n/f", min=0.0, max=1.0) == 0.55
        assert k.get_float("n/f", 0.5, min=0.6) == 0.5
        assert k.get_float("n/nan", 0.5) == 0.5
        assert k.get_bool("n/yes") is True
        assert k.get_bool("n/off") is False
        assert k.get_bool("n/bad", True) is True
    finally:
        k.close()


def test_folder_scoped_listener(server):
    k = connect(server)
    try:
        scoped, nested, all_ = [], [], []
        k.on_change(lambda key, value, folders, source, delta: scoped.append(key), folder="trading")
        k.on_change(lambda key, value: nested.append(key), folder="voice/vad")
        k.on_change(lambda *a: all_.append(a[0]))
        server.push_value(["trading"], "halt", "true")
        server.push_value(["voice", "vad"], "silence", "0.5")
        server.push_value([], "greeting", "hi")
        server.push_value(["tradingX"], "a", "1")
        assert wait_for(lambda: len(all_) == 4)
        assert scoped == ["halt"]
        assert nested == ["silence"]
    finally:
        k.close()


# ------------------------------------------------------------------ close from callback
def test_close_from_listener_callback_does_not_raise(server):
    k = connect(server)
    errors, done = [], threading.Event()

    def cb(key, value, *a):
        try:
            k.close()
        except Exception as e:  # 0.2.0: RuntimeError cannot join current thread
            errors.append(e)
        done.set()

    k.on_change(cb)
    server.push_value(["trading"], "halt", "true")
    assert done.wait(5)
    assert errors == []
    assert k.state is S.CLOSED
    assert wait_for(lambda: not k.listener_thread.is_alive(), 3)


def test_state_callbacks_sequence(server):
    k = Kiponos(profile=PROFILE, server_url=server.url, **FAST)
    seq = []
    k.on_state_change(lambda old, new: seq.append(new))
    k.open()
    server.drop_connections()
    assert wait_for(lambda: k.reconnect_count == 1, 5)
    k.close()
    assert seq == [S.READY, S.OFFLINE, S.READY, S.CLOSED]
    assert k.wait_ready(0.1) is False
