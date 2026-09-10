import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "incident-pause");
  assert.equal(DEFAULT, "off");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "shopping_path_live");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("off").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("on").proceed, false);
  assert.equal(decide("on").action, "freeze_shopping_writes");
});
