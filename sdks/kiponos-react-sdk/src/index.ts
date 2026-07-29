/**
 * @kiponos/react — real-time Kiponos SDK (Node / server-first).
 *
 * Primary API (Java parity):
 *   const kip = Kiponos.createFromEnv();
 *   // or singleton:
 *   const kip = Kiponos.createForCurrentTeam();
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

export { KiponosProvider } from "./react/KiponosProvider";
export type { KiponosProviderProps } from "./react/KiponosProvider";
export { useKiponos } from "./react/useKiponos";
export type { UseKiponosResult } from "./react/useKiponos";
export {
  useKiponosValue,
  useKiponosInt,
  splitPath,
} from "./react/useKiponosValue";
export type { UseKiponosValueOptions } from "./react/useKiponosValue";
export {
  useAfterValueUpdated,
  useAfterKeyCreated,
  useAfterKeyDeleted,
  useAfterKeyRenamed,
  useAfterItemSaved,
  useAfterFolderCreated,
  useKiponosOnChange,
} from "./react/useKiponosListener";
