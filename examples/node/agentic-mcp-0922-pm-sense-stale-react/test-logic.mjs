import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "priority");
  assert.equal(DEFAULT, "P3");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "continue_turn");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("P3").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("P1").proceed, false);
  assert.equal(decide("P1").action, "abort_mid_turn_no_restart");
});
