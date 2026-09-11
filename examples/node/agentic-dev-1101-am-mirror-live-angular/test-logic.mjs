import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "device-live");
  assert.equal(DEFAULT, "yes");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "mirror_device_live");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("yes").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("no").proceed, false);
  assert.equal(decide("no").action, "route_other_mirror_device");
});
