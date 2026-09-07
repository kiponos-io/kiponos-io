import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "skill-trust");
  assert.equal(DEFAULT, "core,reviewed");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "honor_trusted_skills");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("core,reviewed").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("core,reviewed").proceed, true);
});
