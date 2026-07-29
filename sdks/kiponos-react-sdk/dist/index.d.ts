import { C as ConfigTree, K as KiponosClient, F as FromEnvOptions, a as KiponosClientPublic, b as KiponosStatus, c as KiponosFolder, d as ConfigFolderCreatedEvent, e as ConfigItemSavedEvent, f as ConfigKeyCreatedEvent, g as ConfigKeyDeletedEvent, h as ConfigKeyRenamedEvent, i as ConfigValUpdatedEvent, O as OnChangeHandler } from './server-04cmbbaU.js';
export { j as ChangeSource, k as ConfigFolderDeletedEvent, l as ConfigNode, D as DEFAULT_PROFILE, m as Kiponos, n as KiponosClientOptions, T as TeamInfo, U as Unsubscribe, o as foldersFromBase, p as isValidBracketProfile, q as joinPath, r as parseDottedOrSlash, s as profileToBasePath, t as resolveCredentialsFromEnv } from './server-04cmbbaU.js';
import * as react from 'react';
import { ReactNode } from 'react';

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

interface KiponosProviderProps {
    children: ReactNode;
    /**
     * Preferred: inject a server-created client (createFromEnv in Node, pass down).
     * Required for browser UIs — never put Connect tokens in the SPA.
     */
    client?: KiponosClient;
    /**
     * Node-only: create via createFromEnv when no client prop.
     * Throws in the browser if client is omitted.
     */
    fromEnv?: boolean | FromEnvOptions;
    /** Auto connect (default true). */
    autoConnect?: boolean;
}
/**
 * App-level provider. Credentials never come from React props —
 * only process env (Node) or an injected client from your BFF.
 */
declare function KiponosProvider({ children, client: externalClient, fromEnv, autoConnect, }: KiponosProviderProps): react.JSX.Element;

interface UseKiponosResult extends Partial<KiponosClientPublic> {
    client: KiponosClient | null;
    status: KiponosStatus;
    ready: boolean;
    error: Error | null;
    /** Convenience aliases */
    get: KiponosClientPublic["get"];
    set: KiponosClientPublic["set"];
    getPath: KiponosClientPublic["getPath"];
    setPath: KiponosClientPublic["setPath"];
    path: (...folders: string[]) => KiponosFolder;
    ensurePath: KiponosClientPublic["ensurePath"];
    afterValueUpdated: KiponosClientPublic["afterValueUpdated"];
    afterKeyCreated: KiponosClientPublic["afterKeyCreated"];
    afterKeyDeleted: KiponosClientPublic["afterKeyDeleted"];
    afterKeyRenamed: KiponosClientPublic["afterKeyRenamed"];
    afterItemSaved: KiponosClientPublic["afterItemSaved"];
    afterFolderCreated: KiponosClientPublic["afterFolderCreated"];
    afterFolderDeleted: KiponosClientPublic["afterFolderDeleted"];
    onChange: KiponosClientPublic["onChange"];
    dump: KiponosClientPublic["dump"];
    listKeys: KiponosClientPublic["listKeys"];
    listFolders: KiponosClientPublic["listFolders"];
    waitUntilReady: KiponosClientPublic["waitUntilReady"];
    teamId: string;
    profile: string;
}
/**
 * Access the hub client: get/set/path + Java-style after* listeners.
 * Methods are stable; re-renders on status change (and tree via Provider tick).
 */
declare function useKiponos(): UseKiponosResult;

interface UseKiponosValueOptions {
    /** Default when key missing. */
    defaultValue?: string;
    /**
     * Folder segments under profile root.
     * Alternative: pass a slash path as the first argument (`"ui/theme"`).
     */
    folders?: string[];
}
/**
 * Live key value — re-renders when this leaf (or any tree change that
 * affects it) updates from any hub participant.
 *
 * @example
 * const theme = useKiponosValue('theme', { folders: ['ui'], defaultValue: 'dark' });
 * const theme2 = useKiponosValue('ui/theme', { defaultValue: 'dark' });
 */
declare function useKiponosValue(keyOrPath: string, options?: UseKiponosValueOptions): string | undefined;
/**
 * Live integer value (Java `getInt` style).
 */
declare function useKiponosInt(keyOrPath: string, defaultValue?: number, folders?: string[]): number;
/**
 * Split path helper for callers that want folders + key separately.
 */
declare function splitPath(path: string): {
    folders: string[];
    key: string;
};

/**
 * Register `afterValueUpdated` for the lifetime of the component.
 * Handler should be stable (useCallback) to avoid re-subscribe churn.
 */
declare function useAfterValueUpdated(handler: (e: ConfigValUpdatedEvent) => void): void;
declare function useAfterKeyCreated(handler: (e: ConfigKeyCreatedEvent) => void): void;
declare function useAfterKeyDeleted(handler: (e: ConfigKeyDeletedEvent) => void): void;
declare function useAfterKeyRenamed(handler: (e: ConfigKeyRenamedEvent) => void): void;
declare function useAfterItemSaved(handler: (e: ConfigItemSavedEvent) => void): void;
declare function useAfterFolderCreated(handler: (e: ConfigFolderCreatedEvent) => void): void;
/** Catch-all change stream (Python `on_change` style). */
declare function useKiponosOnChange(handler: OnChangeHandler): void;

export { ConfigFolderCreatedEvent, ConfigItemSavedEvent, ConfigKeyCreatedEvent, ConfigKeyDeletedEvent, ConfigKeyRenamedEvent, ConfigTree, ConfigValUpdatedEvent, FromEnvOptions, KiponosClient, KiponosClientPublic, KiponosFolder, KiponosProvider, type KiponosProviderProps, KiponosStatus, OnChangeHandler, type UseKiponosResult, type UseKiponosValueOptions, VALUE_KEY, applyValueAt, buildBrowserSdkUrl, buildSdkAuthHeaders, getValueAt, isKeyNode, listFoldersAt, listKeysAt, splitPath, useAfterFolderCreated, useAfterItemSaved, useAfterKeyCreated, useAfterKeyDeleted, useAfterKeyRenamed, useAfterValueUpdated, useKiponos, useKiponosInt, useKiponosOnChange, useKiponosValue };
