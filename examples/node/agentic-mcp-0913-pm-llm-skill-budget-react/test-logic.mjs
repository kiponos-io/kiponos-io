import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "skill-budget");
  assert.equal(DEFAULT, "on");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "generated_skills_ok");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("off").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("on").proceed, false);
  assert.equal(decide("on").action, "generated_skills_blocked");
});
