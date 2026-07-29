/**
 * E2E: connection, bootstrap, auth failures — LIVE PROD hub.
 *
 * Auth-rejection cases use a temp env file: createFromEnv always prefers
 * a file that supplies KIPONOS_ID/ACCESS (Java-style EnvironmentFile), so
 * mutating process.env alone is overwritten by otp-listener.env.
 */
import { writeFileSync, unlinkSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, it, expect, beforeAll, afterAll } from "vitest";
import {
  createLiveClient,
  credsPresent,
  loadLiveCreds,
  assertNoSecretsIn,
  FAMILY_PROFILE,
  ensureE2eEnv,
} from "./harness";
import { Kiponos } from "../../src/core/kiponos";
import { KiponosClient } from "../../src/core/kiponos-client";

const hasCreds = credsPresent();

function writeTempEnv(contents: string): string {
  const p = join(
    tmpdir(),
    `kiponos-react-e2e-${Date.now()}-${Math.random().toString(36).slice(2)}.env`
  );
  writeFileSync(p, contents, "utf8");
  return p;
}

describe.skipIf(!hasCreds)("E2E live connect (PROD)", () => {
  let client: KiponosClient;

  beforeAll(async () => {
    client = createLiveClient();
    await client.connect();
  }, 60000);

  afterAll(() => {
    client?.disconnect();
  });

  it("connects to ready with teamId and profile", () => {
    expect(client.ready).toBe(true);
    expect(client.status).toBe("ready");
    expect(client.error).toBeNull();
    expect(client.teamId).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
    );
    expect(client.profile).toBe(FAMILY_PROFILE);
    expect(client.basePath).toContain("Family-Agent");
    expect(client.basePath).toContain("Alef-Dev");
  });

  it("bootstrap loads a non-empty folder tree", () => {
    const dump = client.dump();
    expect(typeof dump).toBe("object");
    const folders = client.listFolders();
    // Family-Agent tree is expected to have top-level folders (agent, family, …)
    expect(folders.length + client.listKeys().length).toBeGreaterThan(0);
  });

  it("waitUntilReady resolves immediately when already ready", async () => {
    await expect(client.waitUntilReady(2000)).resolves.toBeUndefined();
  });

  it("dump / listKeys / listFolders do not throw", () => {
    expect(() => client.dump()).not.toThrow();
    expect(() => client.listKeys()).not.toThrow();
    expect(() => client.listFolders()).not.toThrow();
    expect(Array.isArray(client.listKeys())).toBe(true);
    expect(Array.isArray(client.listFolders())).toBe(true);
  });
});

describe.skipIf(!hasCreds)("E2E auth rejection (PROD)", () => {
  it("rejects garbage tokens via env (handshake fails)", async () => {
    ensureE2eEnv();
    const envFile = writeTempEnv(
      [
        "KIPONOS_ID=not-a-real-token",
        "KIPONOS_ACCESS=also-fake",
        `KIPONOS=${FAMILY_PROFILE}`,
      ].join("\n")
    );
    Kiponos.resetSingleton();
    const c = Kiponos.createFromEnv({
      envFile,
      requestTimeoutMs: 15000,
      quiet: true,
    });
    try {
      await expect(c.connect()).rejects.toThrow();
      expect(c.ready).toBe(false);
    } finally {
      try {
        unlinkSync(envFile);
      } catch {
        /* ignore */
      }
      c.disconnect();
      Kiponos.resetSingleton();
      ensureE2eEnv();
    }
  }, 30000);

  it("rejects mismatched profile with valid tokens (handshake 500 class)", async () => {
    ensureE2eEnv();
    const creds = loadLiveCreds();
    const envFile = writeTempEnv(
      [
        `KIPONOS_ID=${creds.idToken}`,
        `KIPONOS_ACCESS=${creds.accessToken}`,
        "KIPONOS=['Not-A-Real-App']['9.9.9']['Nowhere']['ghost']",
      ].join("\n")
    );
    Kiponos.resetSingleton();
    const c = Kiponos.createFromEnv({
      envFile,
      requestTimeoutMs: 20000,
      quiet: true,
    });
    try {
      await expect(c.connect()).rejects.toThrow();
      expect(c.ready).toBe(false);
    } finally {
      try {
        unlinkSync(envFile);
      } catch {
        /* ignore */
      }
      c.disconnect();
      Kiponos.resetSingleton();
      ensureE2eEnv();
    }
  }, 35000);

  it("rejects direct token constructor (createFromEnv only)", () => {
    expect(
      () =>
        new KiponosClient({
          profile: FAMILY_PROFILE,
          idToken: "x",
          accessToken: "y",
        })
    ).toThrow(/createFromEnv|Do not construct/);
  });

  it("createFromEnv fails cleanly with bad tokens in env", async () => {
    ensureE2eEnv();
    const creds = loadLiveCreds();
    const envFile = writeTempEnv(
      [
        "KIPONOS_ID=not-a-real-token",
        "KIPONOS_ACCESS=also-fake",
        `KIPONOS=${FAMILY_PROFILE}`,
      ].join("\n")
    );
    Kiponos.resetSingleton();
    const c = Kiponos.createFromEnv({
      envFile,
      requestTimeoutMs: 15000,
      quiet: true,
    });
    try {
      await expect(c.connect()).rejects.toThrow();
      // failure messages must not embed real token values
      const err = c.error;
      if (err) assertNoSecretsIn(err.message, creds);
    } finally {
      try {
        unlinkSync(envFile);
      } catch {
        /* ignore */
      }
      c.disconnect();
      Kiponos.resetSingleton();
      ensureE2eEnv();
    }
  }, 30000);
});
