/**
 * Public factory — Java SDK parity:
 *   Kiponos.createForCurrentTeam()  // singleton
 *   Kiponos.createFromEnv()         // new instance from process env
 */

import { resolveCredentialsFromEnv, type FromEnvOptions } from "./env";
import { KiponosClient } from "./kiponos-client";

let singleton: KiponosClient | null = null;

function buildFromEnv(options: FromEnvOptions = {}): KiponosClient {
  const creds = resolveCredentialsFromEnv(options);
  return new KiponosClient({
    __fromEnv: true,
    profile: creds.profile,
    idToken: creds.idToken,
    accessToken: creds.accessToken,
    serverUrl: creds.serverUrl,
    quiet: options.quiet ?? true,
    autoConnect: options.autoConnect ?? false,
    requestTimeoutMs: options.requestTimeoutMs,
    sdkVersion: options.sdkVersion,
    authMode: "headers",
  });
}

export class Kiponos {
  /**
   * Create a client from process environment (KIPONOS_ID, KIPONOS_ACCESS, KIPONOS).
   * Optional dotenv via options.envFile or KIPONOS_ENV_FILE.
   * Same idea as constructing after reading System.getenv in Java.
   */
  static createFromEnv(options: FromEnvOptions = {}): KiponosClient {
    return buildFromEnv(options);
  }

  /**
   * Java-style singleton for the current process / team connection.
   * First call creates; later calls return the same instance.
   */
  static createForCurrentTeam(options: FromEnvOptions = {}): KiponosClient {
    if (!singleton) {
      singleton = buildFromEnv(options);
    }
    return singleton;
  }

  /** Alias for createFromEnv. */
  static fromEnv(options: FromEnvOptions = {}): KiponosClient {
    return Kiponos.createFromEnv(options);
  }

  /** Drop singleton (tests / graceful shutdown). */
  static resetSingleton(): void {
    if (singleton) {
      try {
        singleton.disconnect();
      } catch {
        /* ignore */
      }
    }
    singleton = null;
  }
}

export type { FromEnvOptions };
