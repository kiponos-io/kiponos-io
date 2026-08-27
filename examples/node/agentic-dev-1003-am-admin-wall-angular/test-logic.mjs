import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "wall-focus");
  assert.equal(DEFAULT, "checkout");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "admin_wall_focus");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("checkout").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("idle").proceed, false);
  assert.equal(decide("idle").action, "admin_wall_idle");
});
