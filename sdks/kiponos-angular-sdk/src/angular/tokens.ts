import { InjectionToken } from "@angular/core";
import type { KiponosClient } from "../core/kiponos-client";
import type { FromEnvOptions } from "../core/env";

/**
 * Injected hub client (created by provideKiponos / KiponosService).
 */
export const KIPONOS_CLIENT = new InjectionToken<KiponosClient | null>(
  "KIPONOS_CLIENT"
);

/**
 * Config for provideKiponos().
 */
export interface ProvideKiponosConfig {
  /**
   * Preferred: inject a server-created client (createFromEnv in Node).
   * Required for browser UIs — never put Connect tokens in the SPA.
   */
  client?: KiponosClient;
  /**
   * Node-only: create via createFromEnv when no client.
   * Throws in the browser if client is omitted.
   */
  fromEnv?: boolean | FromEnvOptions;
  /** Auto connect on service construct (default true). */
  autoConnect?: boolean;
}

export const KIPONOS_CONFIG = new InjectionToken<ProvideKiponosConfig>(
  "KIPONOS_CONFIG"
);
