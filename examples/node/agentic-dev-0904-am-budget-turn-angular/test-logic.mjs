import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "max-tokens");
  assert.equal(DEFAULT, "8000");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "within_token_budget");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("8000").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("0").proceed, false);
  assert.equal(decide("0").action, "stop_turn_budget");
});
