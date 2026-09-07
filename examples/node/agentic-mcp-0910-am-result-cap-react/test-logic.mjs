import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "result-cap");
  assert.equal(DEFAULT, "2000");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "result_projected");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("8000").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("0").proceed, false);
  assert.equal(decide("0").action, "result_too_fat");
});
