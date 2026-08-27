import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "owner-agent");
  assert.equal(DEFAULT, "travel-coordinator");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "honor_chosen_owner");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("travel-coordinator").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("travel-coordinator").proceed, true);
});
