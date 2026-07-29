/**
 * @kiponos/angular — real-time Kiponos SDK (Node / server-first).
 *
 * Primary API (Java parity):
 *   const kip = Kiponos.createFromEnv();
 *   // or singleton:
 *   const kip = Kiponos.createForCurrentTeam();
 *
 * Angular DI:
 *   provideKiponos({ fromEnv: true })  // Node
 *   inject(KiponosService) / injectKiponos()
 *
 * Reads KIPONOS_ID, KIPONOS_ACCESS, KIPONOS from process environment.
 * Not for browser SPA token embedding — use a Node BFF + SSE for UIs.
 */

export { Kiponos } from "./core/kiponos";
export type { FromEnvOptions } from "./core/env";
export {
  resolveCredentialsFromEnv,
  isValidBracketProfile,
  DEFAULT_PROFILE,
} from "./core/env";

export { KiponosClient } from "./core/kiponos-client";
export {
  profileToBasePath,
  joinPath,
  parseDottedOrSlash,
  foldersFromBase,
} from "./core/paths";
export {
  isKeyNode,
  applyValueAt,
  getValueAt,
  listKeysAt,
  listFoldersAt,
  VALUE_KEY,
} from "./core/tree";
export { buildBrowserSdkUrl, buildSdkAuthHeaders } from "./core/stomp";

export type {
  KiponosClientOptions,
  KiponosClientPublic,
  KiponosFolder,
  KiponosStatus,
  ConfigTree,
  ConfigNode,
  ConfigValUpdatedEvent,
  ConfigKeyCreatedEvent,
  ConfigKeyDeletedEvent,
  ConfigKeyRenamedEvent,
  ConfigItemSavedEvent,
  ConfigFolderCreatedEvent,
  ConfigFolderDeletedEvent,
  OnChangeHandler,
  ChangeSource,
  Unsubscribe,
  TeamInfo,
} from "./core/types";

export { KiponosService } from "./angular/kiponos.service";
export { provideKiponos } from "./angular/provide-kiponos";
export { injectKiponos } from "./angular/inject-kiponos";
export {
  KIPONOS_CLIENT,
  KIPONOS_CONFIG,
  type ProvideKiponosConfig,
} from "./angular/tokens";
