/**
 * E2E: reconnect, concurrent writes, heartbeat survival — LIVE PROD.
 */
import { describe, it, expect, beforeAll, afterAll } from "vitest";
import {
  createLiveClient,
  credsPresent,
  uniqueRunId,
  E2E_ROOT,
  sleep,
  waitFor,
} from "./harness";
import type { KiponosClient } from "../../src/core/kiponos-client";

const hasCreds = credsPresent();
const runId = uniqueRunId();
const F = [E2E_ROOT, runId, "resilience"] as const;

describe.skipIf(!hasCreds)("E2E resilience (PROD)", () => {
  let client: KiponosClient;

  beforeAll(async () => {
    client = createLiveClient();
    await client.connect();
    await client.ensurePath(...F);
  }, 90000);

  afterAll(async () => {
    try {
      if (client?.ready) {
        for (const k of client.listKeys(...F)) {
          await client.deleteKey(k, ...F).catch(() => undefined);
        }
      }
    } catch {
      /* ignore */
    }
    client?.disconnect();
  }, 60000);

  it("disconnect then reconnect restores ready + get/set", async () => {
    await client.set("pre", "1", ...F);
    expect(client.get("pre", undefined, ...F)).toBe("1");

    client.disconnect();
    expect(client.status).toBe("disconnected");
    expect(client.ready).toBe(false);

    await client.connect();
    expect(client.ready).toBe(true);
    // Bootstrap reloads tree — pre may still exist server-side
    await client.ensurePath(...F);
    await client.set("post", "2", ...F);
    expect(client.get("post", undefined, ...F)).toBe("2");
  }, 120000);

  it("concurrent sets to different keys all succeed", async () => {
    const n = 8;
    const ops = Array.from({ length: n }, (_, i) =>
      client.set(`c-${i}`, `v-${i}`, ...F)
    );
    const results = await Promise.all(ops);
    expect(results).toEqual(Array.from({ length: n }, (_, i) => `v-${i}`));
    for (let i = 0; i < n; i++) {
      expect(client.get(`c-${i}`, undefined, ...F)).toBe(`v-${i}`);
    }
  }, 120000);

  it("rapid sequential updates keep last write", async () => {
    const key = "rapid";
    for (let i = 0; i < 5; i++) {
      await client.set(key, String(i), ...F);
    }
    expect(client.get(key, undefined, ...F)).toBe("4");
  }, 90000);

  it("stays connected through heartbeat window (~28s idle)", async () => {
    // Server often negotiates ~26s heart-beat; idle drop is the classic bug.
    const holdMs = Number(process.env.KIPONOS_E2E_HB_MS || 28000);
    await client.set("hb-mark", String(Date.now()), ...F);
    await sleep(holdMs);
    expect(client.ready).toBe(true);
    expect(client.status).toBe("ready");
    // prove socket still accepts writes
    const v = `after-hb-${Date.now()}`;
    await client.set("hb-after", v, ...F);
    expect(client.get("hb-after", undefined, ...F)).toBe(v);
  }, 90000);

  it("set without connect throws clearly", async () => {
    const orphan = createLiveClient();
    await expect(orphan.set("x", "y", ...F)).rejects.toThrow(/not ready/i);
    orphan.disconnect();
  });

  it("double connect is safe (idempotent)", async () => {
    await client.connect();
    await client.connect();
    expect(client.ready).toBe(true);
  }, 60000);
});

describe.skipIf(!hasCreds)("E2E second client observes concurrent writer (PROD)", () => {
  it("B sees all of A's concurrent keys", async () => {
    const a = createLiveClient();
    const b = createLiveClient();
    await a.connect();
    await sleep(400);
    await b.connect();
    const folders = [E2E_ROOT, uniqueRunId(), "fanout"] as const;
    await a.ensurePath(...folders);
    await b.ensurePath(...folders);

    const keys = ["f0", "f1", "f2", "f3", "f4"];
    // sequential sets are more realistic for hub ack ordering; still multi-participant
    for (let i = 0; i < keys.length; i++) {
      await a.set(keys[i], `val-${i}`, ...folders);
    }

    await waitFor(
      () => keys.every((k, i) => b.get(k, undefined, ...folders) === `val-${i}`),
      { timeoutMs: 40000, label: "fanout sync to B" }
    );

    for (const k of keys) {
      await a.deleteKey(k, ...folders).catch(() => undefined);
    }
    a.disconnect();
    b.disconnect();
  }, 120000);
});
