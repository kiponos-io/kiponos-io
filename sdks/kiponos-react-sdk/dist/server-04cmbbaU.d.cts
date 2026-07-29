/**
 * Load Connect identity from process environment only (Java SDK parity).
 * Node-only module (uses node:fs).
 */
declare const DEFAULT_PROFILE = "['Family-Agent']['1.0.0']['Alef-Dev']['base']";
type FromEnvOptions = {
    envFile?: string;
    serverUrl?: string;
    quiet?: boolean;
    autoConnect?: boolean;
    requestTimeoutMs?: number;
    sdkVersion?: string;
};
type ResolvedEnvCredentials = {
    idToken: string;
    accessToken: string;
    profile: string;
    serverUrl: string;
};
declare function isValidBracketProfile(p: string): boolean;
declare function resolveCredentialsFromEnv(options?: FromEnvOptions): ResolvedEnvCredentials;

/** Connection / lifecycle status. */
type KiponosStatus = "idle" | "connecting" | "ready" | "error" | "disconnected";
/** Config tree node: key leaf is `{ value }` or nested folder map. */
type ConfigNode = {
    value: string;
} | ConfigTree;
type ConfigTree = {
    [key: string]: ConfigNode;
};
interface TeamInfo {
    teamId?: string;
    [key: string]: unknown;
}
/**
 * @internal Resolved options after createFromEnv().
 * Apps must not construct clients with raw tokens — use Kiponos.createFromEnv().
 */
interface KiponosClientOptions {
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
interface ConfigValUpdatedEvent {
    key: string;
    value?: string;
    basePath?: string;
    [key: string]: unknown;
}
interface ConfigKeyCreatedEvent {
    key: string;
    basePath?: string;
    [key: string]: unknown;
}
interface ConfigKeyDeletedEvent {
    key: string;
    basePath?: string;
    requestId?: string;
    [key: string]: unknown;
}
interface ConfigKeyRenamedEvent {
    key?: string;
    oldKey?: string;
    newKey?: string;
    basePath?: string;
    [key: string]: unknown;
}
interface ConfigItemSavedEvent {
    key: string;
    value?: string;
    basePath?: string;
    requestId?: string;
    [key: string]: unknown;
}
interface ConfigFolderCreatedEvent {
    folder?: string;
    folderName?: string;
    path?: string;
    basePath?: string;
    requestId?: string;
    [key: string]: unknown;
}
interface ConfigFolderDeletedEvent {
    folder?: string;
    folderName?: string;
    path?: string;
    basePath?: string;
    requestId?: string;
    [key: string]: unknown;
}
type ChangeSource = "config-val-updated" | "config-prop-saved" | "config-key-created" | "config-key-deleted" | "config-key-renamed" | "config-folder-created" | "config-folder-deleted" | string;
type OnChangeHandler = (key: string, value: string | undefined, folders: readonly string[], source: ChangeSource, delta: Record<string, unknown>) => void;
type Unsubscribe = () => void;
interface KiponosFolder {
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
interface KiponosClientPublic {
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
    set(key: string, value: string | number | boolean, ...folders: string[]): Promise<string>;
    setPath(path: string, value: string | number | boolean): Promise<string>;
    path(...folders: string[]): KiponosFolder;
    ensurePath(...folders: string[]): Promise<void>;
    mkdir(folderName: string, ...parentFolders: string[]): Promise<string>;
    deleteKey(key: string, ...folders: string[]): Promise<string>;
    deleteFolder(folderName: string, ...parentFolders: string[]): Promise<string | null>;
    listKeys(...folders: string[]): string[];
    listFolders(...folders: string[]): string[];
    dump(...folders: string[]): ConfigTree;
    afterValueUpdated(handler: (e: ConfigValUpdatedEvent) => void): Unsubscribe;
    afterKeyCreated(handler: (e: ConfigKeyCreatedEvent) => void): Unsubscribe;
    afterKeyDeleted(handler: (e: ConfigKeyDeletedEvent) => void): Unsubscribe;
    afterKeyRenamed(handler: (e: ConfigKeyRenamedEvent) => void): Unsubscribe;
    afterItemSaved(handler: (e: ConfigItemSavedEvent) => void): Unsubscribe;
    afterFolderCreated(handler: (e: ConfigFolderCreatedEvent) => void): Unsubscribe;
    afterFolderDeleted(handler: (e: ConfigFolderDeletedEvent) => void): Unsubscribe;
    onChange(handler: OnChangeHandler): Unsubscribe;
    /** Internal: subscribe to any tree mutation for React. */
    onTreeChanged(handler: () => void): Unsubscribe;
    waitUntilReady(timeoutMs?: number): Promise<void>;
}

/**
 * Framework-agnostic Kiponos real-time client (Java/Python SDK parity).
 */
declare class KiponosClient implements KiponosClientPublic {
    readonly profile: string;
    readonly basePath: string;
    private readonly opts;
    private stomp;
    private configTree;
    private teamIdInternal;
    private statusInternal;
    private errorInternal;
    private subSeq;
    private readonly handlersBySub;
    private readonly pending;
    private readonly listeners;
    private connectPromise;
    private readyWaiters;
    /**
     * @internal Use Kiponos.createFromEnv() / createForCurrentTeam() instead.
     */
    constructor(options: KiponosClientOptions);
    get status(): KiponosStatus;
    get ready(): boolean;
    get error(): Error | null;
    get teamId(): string;
    private setStatus;
    private log;
    /** Run after the current STOMP handler stack (avoids set↔listener reentrancy). */
    private defer;
    private fireTree;
    private emit;
    onStatus(handler: (s: KiponosStatus) => void): Unsubscribe;
    onTreeChanged(handler: () => void): Unsubscribe;
    onChange(handler: OnChangeHandler): Unsubscribe;
    afterValueUpdated(handler: (e: ConfigValUpdatedEvent) => void): Unsubscribe;
    afterKeyCreated(handler: (e: ConfigKeyCreatedEvent) => void): Unsubscribe;
    afterKeyDeleted(handler: (e: ConfigKeyDeletedEvent) => void): Unsubscribe;
    afterKeyRenamed(handler: (e: ConfigKeyRenamedEvent) => void): Unsubscribe;
    afterItemSaved(handler: (e: ConfigItemSavedEvent) => void): Unsubscribe;
    afterFolderCreated(handler: (e: ConfigFolderCreatedEvent) => void): Unsubscribe;
    afterFolderDeleted(handler: (e: ConfigFolderDeletedEvent) => void): Unsubscribe;
    waitUntilReady(timeoutMs?: number): Promise<void>;
    private resolveReadyWaiters;
    private rejectReadyWaiters;
    connect(): Promise<void>;
    private resolveAuth;
    private doConnect;
    private handleBootstrap;
    private subsTopic;
    private handleFrame;
    private onKeyCreated;
    private onValUpdated;
    private onPropSaved;
    private onFolderCreated;
    private onFolderDeleted;
    private onKeyDeleted;
    private onKeyRenamed;
    private newRequestId;
    private wait;
    /**
     * Wait for ack; on timeout return `fallback` instead of throwing.
     * Needed when server skips broadcasts for already-existing folders/keys.
     */
    private waitOr;
    private completePending;
    private failAllPending;
    private ensureConnected;
    get(key: string, defaultValue?: string, ...folders: string[]): string | undefined;
    getPath(path: string, defaultValue?: string): string | undefined;
    set(key: string, value: string | number | boolean, ...folders: string[]): Promise<string>;
    setPath(path: string, value: string | number | boolean): Promise<string>;
    mkdir(folderName: string, ...parentFolders: string[]): Promise<string>;
    ensurePath(...folders: string[]): Promise<void>;
    deleteKey(key: string, ...folders: string[]): Promise<string>;
    deleteFolder(folderName: string, ...parentFolders: string[]): Promise<string | null>;
    listKeys(...folders: string[]): string[];
    listFolders(...folders: string[]): string[];
    dump(...folders: string[]): ConfigTree;
    path(...folders: string[]): KiponosFolder;
    private makeFolder;
    disconnect(): void;
}

/**
 * Public factory — Java SDK parity:
 *   Kiponos.createForCurrentTeam()  // singleton
 *   Kiponos.createFromEnv()         // new instance from process env
 */

declare class Kiponos {
    /**
     * Create a client from process environment (KIPONOS_ID, KIPONOS_ACCESS, KIPONOS).
     * Optional dotenv via options.envFile or KIPONOS_ENV_FILE.
     * Same idea as constructing after reading System.getenv in Java.
     */
    static createFromEnv(options?: FromEnvOptions): KiponosClient;
    /**
     * Java-style singleton for the current process / team connection.
     * First call creates; later calls return the same instance.
     */
    static createForCurrentTeam(options?: FromEnvOptions): KiponosClient;
    /** Alias for createFromEnv. */
    static fromEnv(options?: FromEnvOptions): KiponosClient;
    /** Drop singleton (tests / graceful shutdown). */
    static resetSingleton(): void;
}

/**
 * Profile and JsonPath helpers — aligned with Python agent_client + Java SDK.
 */
/** `['App']['rel']['env']['cfg']` → `$.rootAccount['apps']…` base JsonPath. */
declare function profileToBasePath(profile: string): string;
/** Append folder segments to a JsonPath base. */
declare function joinPath(base: string, ...folders: string[]): string;
/** `'a/b/c'` or `'a.b.c'` → `['a','b','c']`. */
declare function parseDottedOrSlash(path: string): string[];
/** Relative folders under basePath from an absolute basePath field. */
declare function foldersFromBase(basePath: string, eventBase: string | undefined): string[];

export { type ConfigTree as C, DEFAULT_PROFILE as D, type FromEnvOptions as F, KiponosClient as K, type OnChangeHandler as O, type TeamInfo as T, type Unsubscribe as U, type KiponosClientPublic as a, type KiponosStatus as b, type KiponosFolder as c, type ConfigFolderCreatedEvent as d, type ConfigItemSavedEvent as e, type ConfigKeyCreatedEvent as f, type ConfigKeyDeletedEvent as g, type ConfigKeyRenamedEvent as h, type ConfigValUpdatedEvent as i, type ChangeSource as j, type ConfigFolderDeletedEvent as k, type ConfigNode as l, Kiponos as m, type KiponosClientOptions as n, foldersFromBase as o, isValidBracketProfile as p, joinPath as q, parseDottedOrSlash as r, profileToBasePath as s, resolveCredentialsFromEnv as t };
