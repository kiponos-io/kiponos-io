/**
 * Load Connect identity from process environment only (Java SDK parity).
 * Node-only module (uses node:fs).
 */

import { existsSync, readFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";

export const DEFAULT_PROFILE =
  "['Family-Agent']['1.0.0']['Alef-Dev']['base']";

export type FromEnvOptions = {
  envFile?: string;
  serverUrl?: string;
  quiet?: boolean;
  autoConnect?: boolean;
  requestTimeoutMs?: number;
  sdkVersion?: string;
};

export type ResolvedEnvCredentials = {
  idToken: string;
  accessToken: string;
  profile: string;
  serverUrl: string;
};

function isBrowserRuntime(): boolean {
  return (
    typeof globalThis !== "undefined" &&
    typeof (globalThis as { document?: unknown }).document !== "undefined"
  );
}

function isNodeRuntime(): boolean {
  return (
    typeof process !== "undefined" &&
    typeof process.versions === "object" &&
    !!process.versions?.node
  );
}

export function isValidBracketProfile(p: string): boolean {
  return /\['[^']+'\]\['[^']+'\]\['[^']+'\]\['[^']+'\]/.test(p || "");
}

export function parseDotEnvFile(filePath: string): Record<string, string> {
  if (!existsSync(filePath)) return {};
  const out: Record<string, string> = {};
  for (const line of readFileSync(filePath, "utf8").split("\n")) {
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

/**
 * Merge dotenv into process.env.
 * - Default: do not clobber existing env.
 * - When file supplies KIPONOS_ID (Connect tokens), always take KIPONOS /
 *   KIPONOS_PROFILE from the same file so shell leftovers (e.g. Unit-Tests)
 *   cannot pair with Family-Agent tokens → handshake 500.
 */
export function loadEnvFileIntoProcess(filePath: string): void {
  const parsed = parseDotEnvFile(filePath);
  const fileHasTokens = Boolean(parsed.KIPONOS_ID && parsed.KIPONOS_ACCESS);
  for (const [k, v] of Object.entries(parsed)) {
    const isProfileKey = k === "KIPONOS" || k === "KIPONOS_PROFILE";
    if (fileHasTokens && isProfileKey) {
      process.env[k] = v;
      continue;
    }
    if (fileHasTokens && (k === "KIPONOS_ID" || k === "KIPONOS_ACCESS")) {
      process.env[k] = v;
      continue;
    }
    if (process.env[k] === undefined || process.env[k] === "") {
      process.env[k] = v;
    }
  }
}

function resolveEnvFilePath(explicit?: string): string | undefined {
  const fromOpt = explicit || process.env.KIPONOS_ENV_FILE || "";
  if (fromOpt && existsSync(fromOpt)) return fromOpt;
  const defaultOtp = join(homedir(), ".config/kiponos/otp-listener.env");
  if (existsSync(defaultOtp)) return defaultOtp;
  return undefined;
}

export function resolveCredentialsFromEnv(
  options: FromEnvOptions = {}
): ResolvedEnvCredentials {
  if (isBrowserRuntime() || !isNodeRuntime()) {
    throw new Error(
      "[@kiponos/angular] createFromEnv is Node/server-only. " +
        "Browser SPAs must not hold Connect tokens — use a Node BFF/SSE backend " +
        "that calls Kiponos.createFromEnv()."
    );
  }

  const envFile = resolveEnvFilePath(options.envFile);
  if (envFile) {
    loadEnvFileIntoProcess(envFile);
  }

  const idToken = (process.env.KIPONOS_ID || "").trim();
  const accessToken = (process.env.KIPONOS_ACCESS || "").trim();
  if (!idToken || !accessToken) {
    throw new Error(
      "[@kiponos/angular] Missing KIPONOS_ID and/or KIPONOS_ACCESS in process environment. " +
        "Set them like the Java SDK (export or systemd EnvironmentFile). " +
        "Optional: KIPONOS_ENV_FILE pointing at a dotenv file."
    );
  }

  const candidates = [
    process.env.KIPONOS,
    process.env.KIPONOS_PROFILE,
    DEFAULT_PROFILE,
  ];
  let profile = "";
  for (const c of candidates) {
    if (c && isValidBracketProfile(c)) {
      profile = c;
      break;
    }
  }
  if (!profile) {
    throw new Error(
      "[@kiponos/angular] Invalid or missing KIPONOS profile. " +
        "Expected ['App']['Release']['Env']['Config'], e.g. " +
        DEFAULT_PROFILE
    );
  }

  const serverUrl =
    options.serverUrl ||
    process.env.KIPONOS_SERVER ||
    process.env.KIPONOS_WS ||
    "wss://kiponos.io/api/io-kiponos-sdk";

  return { idToken, accessToken, profile, serverUrl };
}
