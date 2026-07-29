/**
 * Live E2E harness — real PROD Kiponos hub via createFromEnv (no token ctor).
 */
import { existsSync, readFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";
import { Kiponos } from "../../src/core/kiponos";
import type { KiponosClient } from "../../src/core/kiponos-client";
import type { FromEnvOptions } from "../../src/core/env";
import { isValidBracketProfile, DEFAULT_PROFILE } from "../../src/core/env";

export const FAMILY_PROFILE = DEFAULT_PROFILE;
export const DEFAULT_SERVER = "wss://kiponos.io/api/io-kiponos-sdk";
export const E2E_ROOT = "e2e-angular-sdk";

export { isValidBracketProfile };

function loadDotEnvFile(path: string): Record<string, string> {
  if (!existsSync(path)) return {};
  const out: Record<string, string> = {};
  for (const line of readFileSync(path, "utf8").split("\n")) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const eq = t.indexOf("=");
    if (eq < 1) continue;
    const k = t.slice(0, eq).trim();
    let v = t.slice(eq + 1).trim();
    if (
      (v.startsWith('"') && v.endsWith('"')) ||
      (v.startsWith("'") && v.endsWith("'"))
    ) {
      v = v.slice(1, -1);
    }
    out[k] = v;
  }
  return out;
}

/** Ensure process.env has Family-Agent tokens (file preferred over shell junk). */
export function ensureE2eEnv(): void {
  const fileEnv = loadDotEnvFile(
    join(homedir(), ".config/kiponos/otp-listener.env")
  );
  if (fileEnv.KIPONOS_ID) process.env.KIPONOS_ID = fileEnv.KIPONOS_ID;
  if (fileEnv.KIPONOS_ACCESS) process.env.KIPONOS_ACCESS = fileEnv.KIPONOS_ACCESS;
  // Force matching profile — never inherit Unit-Tests from shell
  if (fileEnv.KIPONOS && isValidBracketProfile(fileEnv.KIPONOS)) {
    process.env.KIPONOS = fileEnv.KIPONOS;
  } else {
    process.env.KIPONOS = FAMILY_PROFILE;
  }
}

export function credsPresent(): boolean {
  try {
    ensureE2eEnv();
    return !!(process.env.KIPONOS_ID && process.env.KIPONOS_ACCESS);
  } catch {
    return false;
  }
}

export function uniqueRunId(): string {
  return `r${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

export function createLiveClient(
  overrides: FromEnvOptions = {}
): KiponosClient {
  ensureE2eEnv();
  Kiponos.resetSingleton();
  return Kiponos.createFromEnv({
    quiet: process.env.KIPONOS_E2E_VERBOSE === "1" ? false : true,
    requestTimeoutMs: 45000,
    autoConnect: false,
    ...overrides,
  });
}

export async function waitFor(
  predicate: () => boolean,
  opts: { timeoutMs?: number; intervalMs?: number; label?: string } = {}
): Promise<void> {
  const timeoutMs = opts.timeoutMs ?? 20000;
  const intervalMs = opts.intervalMs ?? 50;
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    if (predicate()) return;
    await sleep(intervalMs);
  }
  throw new Error(
    `waitFor timeout${opts.label ? ` (${opts.label})` : ""} after ${timeoutMs}ms`
  );
}

export function sleep(ms: number): Promise<void> {
  return new Promise((r) => setTimeout(r, ms));
}

export function onceValue(
  client: KiponosClient,
  match: (
    key: string,
    value: string | undefined,
    folders: readonly string[]
  ) => boolean,
  timeoutMs = 25000
): Promise<{
  key: string;
  value: string | undefined;
  folders: readonly string[];
  source: string;
}> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      unsub();
      reject(new Error("onceValue timeout"));
    }, timeoutMs);
    const unsub = client.onChange((key, value, folders, source) => {
      if (match(key, value, folders)) {
        clearTimeout(timer);
        unsub();
        resolve({ key, value, folders, source });
      }
    });
  });
}

export function loadLiveCreds(): {
  idToken: string;
  accessToken: string;
  profile: string;
  serverUrl: string;
} {
  ensureE2eEnv();
  return {
    idToken: process.env.KIPONOS_ID || "",
    accessToken: process.env.KIPONOS_ACCESS || "",
    profile: process.env.KIPONOS || FAMILY_PROFILE,
    serverUrl: DEFAULT_SERVER,
  };
}

export function assertNoSecretsIn(
  text: string,
  creds: { idToken: string; accessToken: string }
): void {
  if (text.includes(creds.idToken) || text.includes(creds.accessToken)) {
    throw new Error("SECURITY: token value leaked into log/message string");
  }
}
