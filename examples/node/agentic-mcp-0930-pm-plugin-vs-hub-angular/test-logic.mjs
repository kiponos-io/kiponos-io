import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "enabled-set");
  assert.equal(DEFAULT, "research,notify");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "honor_enabled_skills");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("research,notify").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("research,notify").proceed, true);
});
