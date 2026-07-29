/**
 * E2E: real-time listeners across two independent participants — LIVE PROD.
 */
import { describe, it, expect, beforeAll, afterAll } from "vitest";
import {
  createLiveClient,
  credsPresent,
  uniqueRunId,
  E2E_ROOT,
  onceValue,
  waitFor,
  sleep,
} from "./harness";
import type { KiponosClient } from "../../src/core/kiponos-client";
import type {
  ConfigItemSavedEvent,
  ConfigValUpdatedEvent,
} from "../../src/core/types";

const hasCreds = credsPresent();
const runId = uniqueRunId();
const F = [E2E_ROOT, runId, "bus"] as const;

describe.skipIf(!hasCreds)("E2E multi-participant live events (PROD)", () => {
  let writer: KiponosClient;
  let reader: KiponosClient;

  beforeAll(async () => {
    writer = createLiveClient();
    reader = createLiveClient();
    // Sequential connect avoids server handshake races on same SDK identity
    await writer.connect();
    await sleep(400);
    await reader.connect();
    expect(writer.teamId).toBe(reader.teamId);
    await writer.ensurePath(...F);
    // Local-only path ensure on reader (folder may already exist server-side)
    await reader.ensurePath(...F);
    await sleep(500);
  }, 120000);

  afterAll(async () => {
    try {
      if (writer?.ready) {
        for (const k of writer.listKeys(...F)) {
          await writer.deleteKey(k, ...F).catch(() => undefined);
        }
      }
    } catch {
      /* ignore */
    }
    writer?.disconnect();
    reader?.disconnect();
  }, 60000);

  it("writer set is visible on writer local tree immediately after ack", async () => {
    const v = `w-${Date.now()}`;
    await writer.set("self-read", v, ...F);
    expect(writer.get("self-read", undefined, ...F)).toBe(v);
  }, 60000);

  it("reader receives onChange when writer sets a key", async () => {
    const key = "cross-a";
    const value = `from-writer-${Date.now()}`;
    const pending = onceValue(
      reader,
      (k, val, folders) =>
        k === key &&
        val === value &&
        folders.join("/") === F.join("/")
    );
    // small delay so subscription is armed
    await sleep(100);
    await writer.set(key, value, ...F);
    const evt = await pending;
    expect(evt.key).toBe(key);
    expect(evt.value).toBe(value);
    // reader local tree should apply
    await waitFor(
      () => reader.get(key, undefined, ...F) === value,
      { timeoutMs: 10000, label: "reader local tree apply" }
    );
  }, 90000);

  it("afterItemSaved fires on writer for own save", async () => {
    const key = "saved-hook";
    const value = `sv-${Date.now()}`;
    const seen: ConfigItemSavedEvent[] = [];
    const unsub = writer.afterItemSaved((e) => {
      if (e.key === key) seen.push(e);
    });
    await writer.set(key, value, ...F);
    await waitFor(() => seen.length >= 1, {
      timeoutMs: 20000,
      label: "afterItemSaved",
    });
    unsub();
    expect(seen[0].key).toBe(key);
    expect(String(seen[0].value)).toBe(value);
  }, 60000);

  it("afterValueUpdated can fire on reader for writer updates", async () => {
    // First create key so subsequent update is a value change on some servers
    const key = "val-hook";
    await writer.set(key, "v1", ...F);
    await waitFor(() => reader.get(key, undefined, ...F) === "v1", {
      timeoutMs: 20000,
      label: "seed v1 on reader",
    });

    const seen: ConfigValUpdatedEvent[] = [];
    const unsub = reader.afterValueUpdated((e) => {
      if (e.key === key) seen.push(e);
    });
    // Also catch-all in case server only emits prop-saved for SDK writes
    const any: string[] = [];
    const unsub2 = reader.onChange((k, v, folders, source) => {
      if (k === key && folders.join("/") === F.join("/")) {
        any.push(`${source}:${v}`);
      }
    });

    await writer.set(key, "v2", ...F);
    await waitFor(
      () => reader.get(key, undefined, ...F) === "v2" || any.some((s) => s.endsWith(":v2")),
      { timeoutMs: 25000, label: "reader sees v2" }
    );
    unsub();
    unsub2();
    expect(reader.get(key, undefined, ...F)).toBe("v2");
    // At least one event path must have fired for the reader
    expect(seen.length + any.length).toBeGreaterThan(0);
  }, 90000);

  it("onChange unsub stops further deliveries", async () => {
    let count = 0;
    const unsub = reader.onChange((k) => {
      if (k === "unsub-key") count++;
    });
    await writer.set("unsub-key", "1", ...F);
    await waitFor(() => count >= 1, { timeoutMs: 20000 });
    unsub();
    const at = count;
    await writer.set("unsub-key", "2", ...F);
    await sleep(1500);
    expect(count).toBe(at);
  }, 60000);

  it("delete on writer eventually clears reader local key", async () => {
    const key = "doomed";
    await writer.set(key, "bye", ...F);
    await waitFor(() => reader.get(key, undefined, ...F) === "bye", {
      timeoutMs: 20000,
    });
    await writer.deleteKey(key, ...F);
    await waitFor(() => reader.get(key, "MISSING", ...F) === "MISSING", {
      timeoutMs: 25000,
      label: "reader delete apply",
    });
  }, 90000);
});
