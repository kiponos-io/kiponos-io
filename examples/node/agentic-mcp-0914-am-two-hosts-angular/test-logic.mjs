import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "session-posture");
  assert.equal(DEFAULT, "focus=admin-wall,shopping-pause=off");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "share_session_posture");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("focus=admin-wall,shopping-pause=off").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("focus=admin-wall,shopping-pause=on").proceed, false);
  assert.equal(decide("focus=admin-wall,shopping-pause=on").action, "incident_pause_active");
});
