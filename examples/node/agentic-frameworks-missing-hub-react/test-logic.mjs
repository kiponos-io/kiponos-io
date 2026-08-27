import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "shared-truth");
  assert.equal(DEFAULT, "live");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "peers_share_live_hub");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("live").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("stale").proceed, false);
  assert.equal(decide("stale").action, "refuse_stale_host_argv");
});
