import { useCallback, useMemo } from "react";
import type { KiponosClient } from "../core/kiponos-client";
import type {
  KiponosClientPublic,
  KiponosFolder,
  KiponosStatus,
} from "../core/types";
import { useKiponosContext } from "./context";

export interface UseKiponosResult extends Partial<KiponosClientPublic> {
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
export function useKiponos(): UseKiponosResult {
  const { client, status, ready, error } = useKiponosContext();

  const missing = useCallback(() => {
    throw new Error("Kiponos client not available");
  }, []);

  const get = useCallback(
    (key: string, defaultValue?: string, ...folders: string[]) => {
      if (!client) return defaultValue;
      return client.get(key, defaultValue, ...folders);
    },
    [client]
  );

  const set = useCallback(
    async (key: string, value: string | number | boolean, ...folders: string[]) => {
      if (!client) missing();
      return client!.set(key, value, ...folders);
    },
    [client, missing]
  );

  const getPath = useCallback(
    (path: string, defaultValue?: string) => {
      if (!client) return defaultValue;
      return client.getPath(path, defaultValue);
    },
    [client]
  );

  const setPath = useCallback(
    async (path: string, value: string | number | boolean) => {
      if (!client) missing();
      return client!.setPath(path, value);
    },
    [client, missing]
  );

  const path = useCallback(
    (...folders: string[]) => {
      if (!client) missing();
      return client!.path(...folders);
    },
    [client, missing]
  );

  const ensurePath = useCallback(
    async (...folders: string[]) => {
      if (!client) missing();
      return client!.ensurePath(...folders);
    },
    [client, missing]
  );

  return useMemo(
    () => ({
      client,
      status,
      ready,
      error,
      teamId: client?.teamId ?? "",
      profile: client?.profile ?? "",
      get,
      set,
      getPath,
      setPath,
      path,
      ensurePath,
      afterValueUpdated: (h) => client?.afterValueUpdated(h) ?? (() => undefined),
      afterKeyCreated: (h) => client?.afterKeyCreated(h) ?? (() => undefined),
      afterKeyDeleted: (h) => client?.afterKeyDeleted(h) ?? (() => undefined),
      afterKeyRenamed: (h) => client?.afterKeyRenamed(h) ?? (() => undefined),
      afterItemSaved: (h) => client?.afterItemSaved(h) ?? (() => undefined),
      afterFolderCreated: (h) =>
        client?.afterFolderCreated(h) ?? (() => undefined),
      afterFolderDeleted: (h) =>
        client?.afterFolderDeleted(h) ?? (() => undefined),
      onChange: (h) => client?.onChange(h) ?? (() => undefined),
      dump: (...folders: string[]) => client?.dump(...folders) ?? {},
      listKeys: (...folders: string[]) => client?.listKeys(...folders) ?? [],
      listFolders: (...folders: string[]) =>
        client?.listFolders(...folders) ?? [],
      waitUntilReady: (ms?: number) =>
        client?.waitUntilReady(ms) ?? Promise.reject(new Error("no client")),
      mkdir: (name: string, ...parents: string[]) => {
        if (!client) return Promise.reject(new Error("no client"));
        return client.mkdir(name, ...parents);
      },
      deleteKey: (key: string, ...folders: string[]) => {
        if (!client) return Promise.reject(new Error("no client"));
        return client.deleteKey(key, ...folders);
      },
      deleteFolder: (name: string, ...parents: string[]) => {
        if (!client) return Promise.reject(new Error("no client"));
        return client.deleteFolder(name, ...parents);
      },
    }),
    [
      client,
      status,
      ready,
      error,
      get,
      set,
      getPath,
      setPath,
      path,
      ensurePath,
    ]
  );
}
