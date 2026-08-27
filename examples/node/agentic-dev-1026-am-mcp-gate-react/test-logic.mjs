import assert from "node:assert/strict";
import test from "node:test";
import { KEY, DEFAULT, decide } from "./logic.mjs";

test("leaf constants", () => {
  assert.equal(KEY, "tools-allow");
  assert.equal(DEFAULT, "search,read");
});

test("default proceeds", () => {
  const d = decide(null);
  assert.equal(d.value, DEFAULT);
  assert.equal(d.proceed, true);
  assert.equal(d.action, "allow_listed_tools");
  assert.ok(d.peers.includes("java"));
});

test("ok sample", () => {
  assert.equal(decide("search,read").proceed, true);
});

test("gated sample", () => {
  assert.equal(decide("search,read,write").proceed, false);
  assert.equal(decide("search,read,write").action, "deny_write_no_mcp_restart");
});
