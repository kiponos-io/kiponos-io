/**
 * Node-only entry — no React dependency.
 *   import { Kiponos } from '@kiponos/react/server'
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
export type {
  KiponosClientOptions,
  KiponosClientPublic,
  KiponosFolder,
  KiponosStatus,
  ConfigTree,
  OnChangeHandler,
  Unsubscribe,
} from "./core/types";
