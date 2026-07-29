import {
  makeEnvironmentProviders,
  type EnvironmentProviders,
  type Provider,
} from "@angular/core";
import { KiponosService } from "./kiponos.service";
import {
  KIPONOS_CLIENT,
  KIPONOS_CONFIG,
  type ProvideKiponosConfig,
} from "./tokens";

/**
 * Register Kiponos in an Angular app (standalone or NgModule).
 *
 * @example
 * // Node BFF / SSR — env credentials
 * provideKiponos({ fromEnv: true })
 *
 * // Browser SPA — inject client created on the server (or SSE mirror)
 * provideKiponos({ client: serverClient })
 */
export function provideKiponos(
  config: ProvideKiponosConfig = {}
): EnvironmentProviders {
  const providers: Provider[] = [
    { provide: KIPONOS_CONFIG, useValue: config },
    {
      provide: KIPONOS_CLIENT,
      useValue: config.client ?? null,
    },
    KiponosService,
  ];
  return makeEnvironmentProviders(providers);
}
