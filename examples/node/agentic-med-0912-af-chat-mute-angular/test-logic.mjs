import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "chat-mute");
  assert.equal(DEFAULT, "none");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "group_chat_sends_live");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("none").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("ops-late-bags").proceed, false);
  assert.equal(decide("ops-late-bags").action, "mute_sends_keep_session");
});
