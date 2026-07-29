/**
 * E2E: get/set/path/folders/delete — LIVE PROD, isolated under e2e-react-sdk/<runId>.
 */
import { describe, it, expect, beforeAll, afterAll } from "vitest";
import {
  createLiveClient,
  credsPresent,
  uniqueRunId,
  E2E_ROOT,
  sleep,
} from "./harness";
import type { KiponosClient } from "../../src/core/kiponos-client";

const hasCreds = credsPresent();
const runId = uniqueRunId();
const F = [E2E_ROOT, runId] as const;

describe.skipIf(!hasCreds)("E2E live CRUD + path API (PROD)", () => {
  let client: KiponosClient;

  beforeAll(async () => {
    client = createLiveClient();
    await client.connect();
    await client.ensurePath(...F);
  }, 90000);

  afterAll(async () => {
    // Best-effort cleanup of this run's folder (may leave empty parents)
    try {
      if (client?.ready) {
        const keys = client.listKeys(...F);
        for (const k of keys) {
          await client.deleteKey(k, ...F).catch(() => undefined);
        }
        // nested folders cleanup is best-effort
        const nested = client.listFolders(...F);
        for (const n of nested) {
          const subKeys = client.listKeys(...F, n);
          for (const k of subKeys) {
            await client.deleteKey(k, ...F, n).catch(() => undefined);
          }
          await client.deleteFolder(n, ...F).catch(() => undefined);
        }
      }
    } catch {
      /* ignore cleanup errors */
    }
    client?.disconnect();
  }, 60000);

  it("set + get round-trip at run folder", async () => {
    const key = "hello";
    const value = `world-${runId}`;
    const confirmed = await client.set(key, value, ...F);
    expect(confirmed).toBe(value);
    expect(client.get(key, undefined, ...F)).toBe(value);
    expect(client.listKeys(...F)).toContain(key);
  }, 60000);

  it("idempotent set of same value returns immediately with same value", async () => {
    const key = "idem";
    const value = "same";
    await client.set(key, value, ...F);
    const t0 = Date.now();
    const again = await client.set(key, value, ...F);
    const dt = Date.now() - t0;
    expect(again).toBe(value);
    // Local short-circuit should be fast (no full RTT required)
    expect(dt).toBeLessThan(5000);
  }, 45000);

  it("updates existing key to a new value", async () => {
    const key = "counter-like";
    await client.set(key, "1", ...F);
    await client.set(key, "2", ...F);
    expect(client.get(key, undefined, ...F)).toBe("2");
  }, 60000);

  it("path() fluent get/set matches folder segments", async () => {
    await client.ensurePath(...F, "ui");
    const folder = client.path(...F, "ui");
    await folder.set("theme", "dark");
    expect(folder.get("theme")).toBe("dark");
    expect(client.get("theme", undefined, ...F, "ui")).toBe("dark");
    expect(folder.hasKey("theme")).toBe(true);
  }, 60000);

  it("setPath / getPath slash notation", async () => {
    const p = `${E2E_ROOT}/${runId}/ui/accent`;
    await client.ensurePath(...F, "ui");
    await client.setPath(p, "#00ff99");
    expect(client.getPath(p)).toBe("#00ff99");
  }, 60000);

  it("getInt parses numeric string", async () => {
    await client.set("n", "42", ...F);
    const folder = client.path(...F);
    expect(folder.getInt("n")).toBe(42);
    expect(folder.getInt("missing", 7)).toBe(7);
  }, 45000);

  it("folderOrCreate creates nested folder and allows keys", async () => {
    const nested = await client
      .path(...F)
      .folderOrCreate("deep")
      .then((f) => f.folderOrCreate("nest"));
    await nested.set("leaf", "ok");
    expect(client.get("leaf", undefined, ...F, "deep", "nest")).toBe("ok");
    expect(client.path(...F, "deep").hasFolder("nest")).toBe(true);
  }, 90000);

  it("ensurePath is idempotent", async () => {
    await client.ensurePath(...F, "idem-folder");
    await client.ensurePath(...F, "idem-folder");
    expect(client.listFolders(...F)).toContain("idem-folder");
  }, 60000);

  it("listKeys / listFolders reflect server-backed local tree", async () => {
    await client.set("list-a", "1", ...F);
    await client.set("list-b", "2", ...F);
    await client.ensurePath(...F, "list-folder");
    const keys = client.listKeys(...F);
    expect(keys).toEqual(expect.arrayContaining(["list-a", "list-b"]));
    expect(client.listFolders(...F)).toContain("list-folder");
  }, 60000);

  it("deleteKey removes key from local tree after server ack", async () => {
    await client.set("to-delete", "x", ...F);
    expect(client.get("to-delete", undefined, ...F)).toBe("x");
    await client.deleteKey("to-delete", ...F);
    // allow local apply
    await sleep(200);
    expect(client.get("to-delete", "GONE", ...F)).toBe("GONE");
    expect(client.listKeys(...F)).not.toContain("to-delete");
  }, 60000);

  it("deleteFolder removes empty subfolder", async () => {
    await client.ensurePath(...F, "temp-folder");
    expect(client.listFolders(...F)).toContain("temp-folder");
    await client.deleteFolder("temp-folder", ...F);
    await sleep(300);
    expect(client.listFolders(...F)).not.toContain("temp-folder");
  }, 60000);

  it("coerces number/boolean set values to strings", async () => {
    await client.set("boolish", true, ...F);
    await client.set("numish", 99, ...F);
    expect(client.get("boolish", undefined, ...F)).toBe("true");
    expect(client.get("numish", undefined, ...F)).toBe("99");
  }, 45000);

  it("missing key returns default", () => {
    expect(client.get("no-such-key-xyz", "fallback", ...F)).toBe("fallback");
    expect(client.getPath(`${E2E_ROOT}/${runId}/nope`, "x")).toBe("x");
  });
});
