import { C as ConfigTree, K as KiponosClient, F as FromEnvOptions, a as KiponosStatus, b as KiponosFolder, c as ConfigValUpdatedEvent, U as Unsubscribe, d as ConfigKeyCreatedEvent, e as ConfigKeyDeletedEvent, f as ConfigKeyRenamedEvent, g as ConfigItemSavedEvent, h as ConfigFolderCreatedEvent, i as ConfigFolderDeletedEvent, O as OnChangeHandler } from './server-0ZVwBa9B.js';
export { j as ChangeSource, k as ConfigNode, D as DEFAULT_PROFILE, l as Kiponos, m as KiponosClientOptions, n as KiponosClientPublic, T as TeamInfo, o as foldersFromBase, p as isValidBracketProfile, q as joinPath, r as parseDottedOrSlash, s as profileToBasePath, t as resolveCredentialsFromEnv } from './server-0ZVwBa9B.js';
import { InjectionToken, WritableSignal, Signal, EnvironmentProviders } from '@angular/core';
import { Observable } from 'rxjs';

declare const VALUE_KEY = "value";
declare function isKeyNode(node: unknown): node is {
    value: string;
};
declare function applyValueAt(tree: ConfigTree, folders: string[], key: string, value: string): void;
declare function getValueAt(tree: ConfigTree, folders: string[], key: string, defaultValue?: string): string | undefined;
declare function listKeysAt(tree: ConfigTree, folders: string[]): string[];
declare function listFoldersAt(tree: ConfigTree, folders: string[]): string[];

/**
 * Create a WebSocket for browser or Node.
 * Node: optional HTTP headers (Java/Python parity) via the `ws` package.
 * Browser: native WebSocket (no custom headers — use query auth).
 */
type SdkAuthHeaders = {
    "sdk-id-token": string;
    "sdk-access-token": string;
    "kiponos-id": string;
    "sdk-version": string;
};

/**
 * Minimal STOMP 1.2 over browser/Node WebSocket.
 * Heartbeats + binary bootstrap frames (aligned with Python agent_client).
 */

/** Build browser SDK URL with query auth (headers not available in browsers). */
declare function buildBrowserSdkUrl(serverUrl: string, tokens: {
    idToken: string;
    accessToken: string;
    profile: string;
    sdkVersion: string;
}): string;
declare function buildSdkAuthHeaders(tokens: {
    idToken: string;
    accessToken: string;
    profile: string;
    sdkVersion: string;
}): SdkAuthHeaders;

/**
 * Injected hub client (created by provideKiponos / KiponosService).
 */
declare const KIPONOS_CLIENT: InjectionToken<KiponosClient | null>;
/**
 * Config for provideKiponos().
 */
interface ProvideKiponosConfig {
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
declare const KIPONOS_CONFIG: InjectionToken<ProvideKiponosConfig>;

/**
 * Angular DI surface over KiponosClient — Java SDK API parity:
 * get / set / path / folderOrCreate / after* listeners.
 *
 * Prefer signals for template bindings; Observables for RxJS pipelines.
 */
declare class KiponosService {
    private readonly destroyRef;
    private readonly ownedClient;
    private readonly clientRef;
    /** Connection status (signal). */
    readonly status: WritableSignal<KiponosStatus>;
    /** Last error (signal). */
    readonly error: WritableSignal<Error | null>;
    /** True when status === 'ready'. */
    readonly ready: Signal<boolean>;
    /** Tree mutation epoch — bump on any local/remote tree change. */
    readonly treeEpoch: WritableSignal<number>;
    private readonly status$;
    private readonly change$;
    constructor(externalClient: KiponosClient | null, config: ProvideKiponosConfig | null);
    /** Underlying client (null if not configured). */
    get client(): KiponosClient | null;
    get teamId(): string;
    get profile(): string;
    get basePath(): string;
    /** Status as Observable (for async pipe / RxJS). */
    statusChanges(): Observable<KiponosStatus>;
    /** Catch-all change stream as Observable. */
    changes(): Observable<{
        key: string;
        value: string | undefined;
        folders: readonly string[];
        source: string;
        delta: Record<string, unknown>;
    }>;
    connect(): Promise<void>;
    disconnect(): void;
    waitUntilReady(timeoutMs?: number): Promise<void>;
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
    /**
     * Live leaf as a Signal — re-computes when treeEpoch bumps.
     * Java get style; use in templates: `theme()`.
     *
     * @example
     * theme = this.kip.value('ui/theme', { defaultValue: 'dark' });
     */
    value(keyOrPath: string, options?: {
        defaultValue?: string;
        folders?: string[];
    }): Signal<string | undefined>;
    /**
     * Live integer Signal (Java getInt).
     */
    valueInt(keyOrPath: string, defaultValue?: number, folders?: string[]): Signal<number>;
}

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
declare function provideKiponos(config?: ProvideKiponosConfig): EnvironmentProviders;

/**
 * Convenience inject() for components / services.
 *
 * @example
 * private readonly kip = injectKiponos();
 * theme = this.kip.value('ui/theme', { defaultValue: 'dark' });
 */
declare function injectKiponos(): KiponosService;

export { ConfigFolderCreatedEvent, ConfigFolderDeletedEvent, ConfigItemSavedEvent, ConfigKeyCreatedEvent, ConfigKeyDeletedEvent, ConfigKeyRenamedEvent, ConfigTree, ConfigValUpdatedEvent, FromEnvOptions, KIPONOS_CLIENT, KIPONOS_CONFIG, KiponosClient, KiponosFolder, KiponosService, KiponosStatus, OnChangeHandler, type ProvideKiponosConfig, Unsubscribe, VALUE_KEY, applyValueAt, buildBrowserSdkUrl, buildSdkAuthHeaders, getValueAt, injectKiponos, isKeyNode, listFoldersAt, listKeysAt, provideKiponos };
