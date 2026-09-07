import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "retry-max");
  assert.equal(DEFAULT, "2");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "retry_within_cap");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("8000").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("0").proceed, false);
  assert.equal(decide("0").action, "doom_loop_stopped");
});
