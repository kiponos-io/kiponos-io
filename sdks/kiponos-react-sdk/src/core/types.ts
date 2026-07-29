/** Connection / lifecycle status. */
export type KiponosStatus =
  | "idle"
  | "connecting"
  | "ready"
  | "error"
  | "disconnected";

/** Config tree node: key leaf is `{ value }` or nested folder map. */
export type ConfigNode = { value: string } | ConfigTree;

export type ConfigTree = { [key: string]: ConfigNode };

export interface TeamInfo {
  teamId?: string;
  [key: string]: unknown;
}

/**
 * @internal Resolved options after createFromEnv().
 * Apps must not construct clients with raw tokens — use Kiponos.createFromEnv().
 */
export interface KiponosClientOptions {
  profile: string;
  idToken: string;
  accessToken: string;
  serverUrl?: string;
  sdkVersion?: string;
  requestTimeoutMs?: number;
  /**
   * Handshake mode. createFromEnv always uses `headers` (Node/Java parity).
   */
  authMode?: "headers" | "query" | "auto";
  /** @deprecated */
  browserAuth?: "query";
  quiet?: boolean;
  autoConnect?: boolean;
  /**
   * Factory gate — only set by createFromEnv / createForCurrentTeam.
   * Direct `new KiponosClient({...tokens})` without this throws.
   */
  __fromEnv?: true;
}

/** Delta payload shapes from team topics (subset; server may add fields). */
export interface ConfigValUpdatedEvent {
  key: string;
  value?: string;
  basePath?: string;
  [key: string]: unknown;
}

export interface ConfigKeyCreatedEvent {
  key: string;
  basePath?: string;
  [key: string]: unknown;
}

export interface ConfigKeyDeletedEvent {
  key: string;
  basePath?: string;
  requestId?: string;
  [key: string]: unknown;
}

export interface ConfigKeyRenamedEvent {
  key?: string;
  oldKey?: string;
  newKey?: string;
  basePath?: string;
  [key: string]: unknown;
}

export interface ConfigItemSavedEvent {
  key: string;
  value?: string;
  basePath?: string;
  requestId?: string;
  [key: string]: unknown;
}

export interface ConfigFolderCreatedEvent {
  folder?: string;
  folderName?: string;
  path?: string;
  basePath?: string;
  requestId?: string;
  [key: string]: unknown;
}

export interface ConfigFolderDeletedEvent {
  folder?: string;
  folderName?: string;
  path?: string;
  basePath?: string;
  requestId?: string;
  [key: string]: unknown;
}

export type ChangeSource =
  | "config-val-updated"
  | "config-prop-saved"
  | "config-key-created"
  | "config-key-deleted"
  | "config-key-renamed"
  | "config-folder-created"
  | "config-folder-deleted"
  | string;

export type OnChangeHandler = (
  key: string,
  value: string | undefined,
  folders: readonly string[],
  source: ChangeSource,
  delta: Record<string, unknown>
) => void;

export type Unsubscribe = () => void;

export interface KiponosFolder {
  /** Navigate into subfolders (must exist locally or throw on get). */
  path(...folders: string[]): KiponosFolder;
  /** One segment (Java `folder`). */
  folder(name: string): KiponosFolder;
  /** Create folder if missing; returns folder handle after server ack. */
  folderOrCreate(name: string): Promise<KiponosFolder>;
  get(key: string, defaultValue?: string): string | undefined;
  getInt(key: string, defaultValue?: number): number;
  getLong(key: string, defaultValue?: number): number;
  set(key: string, value: string | number | boolean): Promise<string>;
  hasKey(key: string): boolean;
  hasFolder(name: string): boolean;
  listKeys(): string[];
  listFolders(): string[];
  dump(): ConfigTree;
  /** Path segments under profile root. */
  readonly folders: readonly string[];
}

export interface KiponosClientPublic {
  readonly status: KiponosStatus;
  readonly ready: boolean;
  readonly error: Error | null;
  readonly teamId: string;
  readonly profile: string;
  readonly basePath: string;

  connect(): Promise<void>;
  disconnect(): void;

  get(key: string, defaultValue?: string, ...folders: string[]): string | undefined;
  getPath(path: string, defaultValue?: string): string | undefined;
  set(
    key: string,
    value: string | number | boolean,
    ...folders: string[]
  ): Promise<string>;
  setPath(path: string, value: string | number | boolean): Promise<string>;
  path(...folders: string[]): KiponosFolder;
  ensurePath(...folders: string[]): Promise<void>;
  mkdir(folderName: string, ...parentFolders: string[]): Promise<string>;
  deleteKey(key: string, ...folders: string[]): Promise<string>;
  deleteFolder(folderName: string, ...parentFolders: string[]): Promise<string | null>;
  listKeys(...folders: string[]): string[];
  listFolders(...folders: string[]): string[];
  dump(...folders: string[]): ConfigTree;

  afterValueUpdated(
    handler: (e: ConfigValUpdatedEvent) => void
  ): Unsubscribe;
  afterKeyCreated(handler: (e: ConfigKeyCreatedEvent) => void): Unsubscribe;
  afterKeyDeleted(handler: (e: ConfigKeyDeletedEvent) => void): Unsubscribe;
  afterKeyRenamed(handler: (e: ConfigKeyRenamedEvent) => void): Unsubscribe;
  afterItemSaved(handler: (e: ConfigItemSavedEvent) => void): Unsubscribe;
  afterFolderCreated(
    handler: (e: ConfigFolderCreatedEvent) => void
  ): Unsubscribe;
  afterFolderDeleted(
    handler: (e: ConfigFolderDeletedEvent) => void
  ): Unsubscribe;
  onChange(handler: OnChangeHandler): Unsubscribe;

  /** Internal: subscribe to any tree mutation for React. */
  onTreeChanged(handler: () => void): Unsubscribe;
  waitUntilReady(timeoutMs?: number): Promise<void>;
}
