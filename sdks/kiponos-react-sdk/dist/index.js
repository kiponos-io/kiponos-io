import { Kiponos, parseDottedOrSlash } from './chunk-E333TELH.js';
export { DEFAULT_PROFILE, Kiponos, KiponosClient, VALUE_KEY, applyValueAt, buildBrowserSdkUrl, buildSdkAuthHeaders, foldersFromBase, getValueAt, isKeyNode, isValidBracketProfile, joinPath, listFoldersAt, listKeysAt, parseDottedOrSlash, profileToBasePath, resolveCredentialsFromEnv } from './chunk-E333TELH.js';
import { createContext, useState, useEffect, useMemo, useCallback, useContext } from 'react';
import { jsx } from 'react/jsx-runtime';

var KiponosReactContext = createContext(
  null
);
function useKiponosContext() {
  const ctx = useContext(KiponosReactContext);
  if (!ctx) {
    throw new Error(
      "useKiponos* hooks require <KiponosProvider> in the component tree"
    );
  }
  return ctx;
}
function KiponosProvider({
  children,
  client: externalClient,
  fromEnv = false,
  autoConnect = true
}) {
  const [ownedClient, setOwnedClient] = useState(null);
  const client = externalClient ?? ownedClient;
  const [status, setStatus] = useState(
    client?.status ?? "idle"
  );
  const [error, setError] = useState(client?.error ?? null);
  const [tick, setTick] = useState(0);
  useEffect(() => {
    if (externalClient) return;
    if (!fromEnv) {
      setError(
        new Error(
          "[@kiponos/react] KiponosProvider needs client={...} from createFromEnv() or fromEnv on a Node server runtime."
        )
      );
      return;
    }
    try {
      const opts = typeof fromEnv === "object" ? fromEnv : {};
      const c = Kiponos.createFromEnv({ ...opts, autoConnect: false });
      setOwnedClient(c);
      setStatus(c.status);
      setError(c.error);
      return () => {
        c.disconnect();
        setOwnedClient(null);
      };
    } catch (e) {
      setError(e instanceof Error ? e : new Error(String(e)));
      return;
    }
  }, [externalClient, fromEnv]);
  useEffect(() => {
    if (!client) return;
    const unsubStatus = client.onStatus((s) => {
      setStatus(s);
      setError(client.error);
    });
    const unsubTree = client.onTreeChanged(() => setTick((t) => t + 1));
    if (autoConnect && client.status !== "ready" && client.status !== "connecting") {
      void client.connect().catch((e) => {
        setError(e);
        setStatus("error");
      });
    }
    setStatus(client.status);
    setError(client.error);
    return () => {
      unsubStatus();
      unsubTree();
    };
  }, [client, autoConnect]);
  const value = useMemo(
    () => ({
      client,
      status,
      error,
      ready: status === "ready",
      treeEpoch: tick
    }),
    [client, status, error, tick]
  );
  return /* @__PURE__ */ jsx(KiponosReactContext.Provider, { value, children });
}
function useKiponos() {
  const { client, status, ready, error } = useKiponosContext();
  const missing = useCallback(() => {
    throw new Error("Kiponos client not available");
  }, []);
  const get = useCallback(
    (key, defaultValue, ...folders) => {
      if (!client) return defaultValue;
      return client.get(key, defaultValue, ...folders);
    },
    [client]
  );
  const set = useCallback(
    async (key, value, ...folders) => {
      if (!client) missing();
      return client.set(key, value, ...folders);
    },
    [client, missing]
  );
  const getPath = useCallback(
    (path2, defaultValue) => {
      if (!client) return defaultValue;
      return client.getPath(path2, defaultValue);
    },
    [client]
  );
  const setPath = useCallback(
    async (path2, value) => {
      if (!client) missing();
      return client.setPath(path2, value);
    },
    [client, missing]
  );
  const path = useCallback(
    (...folders) => {
      if (!client) missing();
      return client.path(...folders);
    },
    [client, missing]
  );
  const ensurePath = useCallback(
    async (...folders) => {
      if (!client) missing();
      return client.ensurePath(...folders);
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
      afterValueUpdated: (h) => client?.afterValueUpdated(h) ?? (() => void 0),
      afterKeyCreated: (h) => client?.afterKeyCreated(h) ?? (() => void 0),
      afterKeyDeleted: (h) => client?.afterKeyDeleted(h) ?? (() => void 0),
      afterKeyRenamed: (h) => client?.afterKeyRenamed(h) ?? (() => void 0),
      afterItemSaved: (h) => client?.afterItemSaved(h) ?? (() => void 0),
      afterFolderCreated: (h) => client?.afterFolderCreated(h) ?? (() => void 0),
      afterFolderDeleted: (h) => client?.afterFolderDeleted(h) ?? (() => void 0),
      onChange: (h) => client?.onChange(h) ?? (() => void 0),
      dump: (...folders) => client?.dump(...folders) ?? {},
      listKeys: (...folders) => client?.listKeys(...folders) ?? [],
      listFolders: (...folders) => client?.listFolders(...folders) ?? [],
      waitUntilReady: (ms) => client?.waitUntilReady(ms) ?? Promise.reject(new Error("no client")),
      mkdir: (name, ...parents) => {
        if (!client) return Promise.reject(new Error("no client"));
        return client.mkdir(name, ...parents);
      },
      deleteKey: (key, ...folders) => {
        if (!client) return Promise.reject(new Error("no client"));
        return client.deleteKey(key, ...folders);
      },
      deleteFolder: (name, ...parents) => {
        if (!client) return Promise.reject(new Error("no client"));
        return client.deleteFolder(name, ...parents);
      }
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
      ensurePath
    ]
  );
}
function useKiponosValue(keyOrPath, options = {}) {
  const { client, ready } = useKiponosContext();
  const { defaultValue, folders: foldersOpt } = options;
  const resolve = () => {
    if (!client) return defaultValue;
    if (foldersOpt) {
      return client.get(keyOrPath, defaultValue, ...foldersOpt);
    }
    if (keyOrPath.includes("/") || keyOrPath.includes(".")) {
      return client.getPath(keyOrPath, defaultValue);
    }
    return client.get(keyOrPath, defaultValue);
  };
  const [value, setValue] = useState(resolve);
  useEffect(() => {
    setValue(resolve());
    if (!client) return;
    const unsub = client.onTreeChanged(() => {
      setValue(resolve());
    });
    return unsub;
  }, [client, ready, keyOrPath, defaultValue, foldersOpt?.join("/")]);
  return value;
}
function useKiponosInt(keyOrPath, defaultValue = 0, folders) {
  const raw = useKiponosValue(keyOrPath, {
    defaultValue: String(defaultValue),
    folders
  });
  if (raw === void 0 || raw === "") return defaultValue;
  const n = parseInt(raw, 10);
  return Number.isFinite(n) ? n : defaultValue;
}
function splitPath(path) {
  const parts = parseDottedOrSlash(path);
  if (!parts.length) return { folders: [], key: "" };
  return { folders: parts.slice(0, -1), key: parts[parts.length - 1] };
}
function useAfterValueUpdated(handler) {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterValueUpdated(handler);
  }, [client, handler]);
}
function useAfterKeyCreated(handler) {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterKeyCreated(handler);
  }, [client, handler]);
}
function useAfterKeyDeleted(handler) {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterKeyDeleted(handler);
  }, [client, handler]);
}
function useAfterKeyRenamed(handler) {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterKeyRenamed(handler);
  }, [client, handler]);
}
function useAfterItemSaved(handler) {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterItemSaved(handler);
  }, [client, handler]);
}
function useAfterFolderCreated(handler) {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.afterFolderCreated(handler);
  }, [client, handler]);
}
function useKiponosOnChange(handler) {
  const { client } = useKiponosContext();
  useEffect(() => {
    if (!client) return;
    return client.onChange(handler);
  }, [client, handler]);
}

export { KiponosProvider, splitPath, useAfterFolderCreated, useAfterItemSaved, useAfterKeyCreated, useAfterKeyDeleted, useAfterKeyRenamed, useAfterValueUpdated, useKiponos, useKiponosInt, useKiponosOnChange, useKiponosValue };
//# sourceMappingURL=index.js.map
//# sourceMappingURL=index.js.map