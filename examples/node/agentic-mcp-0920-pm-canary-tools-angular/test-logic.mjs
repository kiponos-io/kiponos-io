import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "canary-percent");
  assert.equal(DEFAULT, "0");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "canary_tools_live");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("8000").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("0").proceed, false);
  assert.equal(decide("0").action, "canary_tools_off");
});
