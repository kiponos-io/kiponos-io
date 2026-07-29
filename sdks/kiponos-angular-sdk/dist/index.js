import { __decorateClass, __decorateParam, Kiponos } from './chunk-5GDSE6H7.js';
export { DEFAULT_PROFILE, Kiponos, KiponosClient, VALUE_KEY, applyValueAt, buildBrowserSdkUrl, buildSdkAuthHeaders, foldersFromBase, getValueAt, isKeyNode, isValidBracketProfile, joinPath, listFoldersAt, listKeysAt, parseDottedOrSlash, profileToBasePath, resolveCredentialsFromEnv } from './chunk-5GDSE6H7.js';
import { InjectionToken, Injectable, Optional, Inject, inject, DestroyRef, signal, computed, makeEnvironmentProviders } from '@angular/core';
import { Subject } from 'rxjs';

var KIPONOS_CLIENT = new InjectionToken(
  "KIPONOS_CLIENT"
);
var KIPONOS_CONFIG = new InjectionToken(
  "KIPONOS_CONFIG"
);

// src/angular/kiponos.service.ts
var KiponosService = class {
  constructor(externalClient, config) {
    this.destroyRef = inject(DestroyRef);
    /** Connection status (signal). */
    this.status = signal("idle");
    /** Last error (signal). */
    this.error = signal(null);
    /** True when status === 'ready'. */
    this.ready = computed(() => this.status() === "ready");
    /** Tree mutation epoch — bump on any local/remote tree change. */
    this.treeEpoch = signal(0);
    this.status$ = new Subject();
    this.change$ = new Subject();
    const cfg = config ?? {};
    let client = externalClient ?? cfg.client ?? null;
    let owned = null;
    if (!client && cfg.fromEnv) {
      try {
        const opts = typeof cfg.fromEnv === "object" ? cfg.fromEnv : {};
        owned = Kiponos.createFromEnv({ ...opts, autoConnect: false });
        client = owned;
      } catch (e) {
        this.error.set(e instanceof Error ? e : new Error(String(e)));
      }
    }
    if (!client && !cfg.fromEnv && !externalClient) {
      this.error.set(
        new Error(
          "[@kiponos/angular] KiponosService needs provideKiponos({ client }) or provideKiponos({ fromEnv: true }) on Node."
        )
      );
    }
    this.ownedClient = owned;
    this.clientRef = client;
    if (client) {
      this.status.set(client.status);
      this.error.set(client.error);
      const unsubStatus = client.onStatus((s) => {
        this.status.set(s);
        this.error.set(client.error);
        this.status$.next(s);
      });
      const unsubTree = client.onTreeChanged(() => {
        this.treeEpoch.update((n) => n + 1);
      });
      const unsubChange = client.onChange((key, value, folders, source, delta) => {
        this.change$.next({ key, value, folders, source, delta });
      });
      const autoConnect = cfg.autoConnect !== false;
      if (autoConnect && client.status !== "ready" && client.status !== "connecting") {
        void client.connect().catch((e) => {
          this.error.set(e);
          this.status.set("error");
        });
      }
      this.destroyRef.onDestroy(() => {
        unsubStatus();
        unsubTree();
        unsubChange();
        if (this.ownedClient) {
          try {
            this.ownedClient.disconnect();
          } catch {
          }
        }
        this.status$.complete();
        this.change$.complete();
      });
    }
  }
  /** Underlying client (null if not configured). */
  get client() {
    return this.clientRef;
  }
  get teamId() {
    return this.clientRef?.teamId ?? "";
  }
  get profile() {
    return this.clientRef?.profile ?? "";
  }
  get basePath() {
    return this.clientRef?.basePath ?? "";
  }
  /** Status as Observable (for async pipe / RxJS). */
  statusChanges() {
    return this.status$.asObservable();
  }
  /** Catch-all change stream as Observable. */
  changes() {
    return this.change$.asObservable();
  }
  async connect() {
    if (!this.clientRef) throw new Error("Kiponos client not available");
    return this.clientRef.connect();
  }
  disconnect() {
    this.clientRef?.disconnect();
  }
  waitUntilReady(timeoutMs) {
    if (!this.clientRef) {
      return Promise.reject(new Error("Kiponos client not available"));
    }
    return this.clientRef.waitUntilReady(timeoutMs);
  }
  get(key, defaultValue, ...folders) {
    if (!this.clientRef) return defaultValue;
    return this.clientRef.get(key, defaultValue, ...folders);
  }
  getPath(path, defaultValue) {
    if (!this.clientRef) return defaultValue;
    return this.clientRef.getPath(path, defaultValue);
  }
  set(key, value, ...folders) {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.set(key, value, ...folders);
  }
  setPath(path, value) {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.setPath(path, value);
  }
  path(...folders) {
    if (!this.clientRef) throw new Error("Kiponos client not available");
    return this.clientRef.path(...folders);
  }
  ensurePath(...folders) {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.ensurePath(...folders);
  }
  mkdir(folderName, ...parentFolders) {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.mkdir(folderName, ...parentFolders);
  }
  deleteKey(key, ...folders) {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.deleteKey(key, ...folders);
  }
  deleteFolder(folderName, ...parentFolders) {
    if (!this.clientRef) return Promise.reject(new Error("no client"));
    return this.clientRef.deleteFolder(folderName, ...parentFolders);
  }
  listKeys(...folders) {
    return this.clientRef?.listKeys(...folders) ?? [];
  }
  listFolders(...folders) {
    return this.clientRef?.listFolders(...folders) ?? [];
  }
  dump(...folders) {
    return this.clientRef?.dump(...folders) ?? {};
  }
  afterValueUpdated(handler) {
    return this.clientRef?.afterValueUpdated(handler) ?? (() => void 0);
  }
  afterKeyCreated(handler) {
    return this.clientRef?.afterKeyCreated(handler) ?? (() => void 0);
  }
  afterKeyDeleted(handler) {
    return this.clientRef?.afterKeyDeleted(handler) ?? (() => void 0);
  }
  afterKeyRenamed(handler) {
    return this.clientRef?.afterKeyRenamed(handler) ?? (() => void 0);
  }
  afterItemSaved(handler) {
    return this.clientRef?.afterItemSaved(handler) ?? (() => void 0);
  }
  afterFolderCreated(handler) {
    return this.clientRef?.afterFolderCreated(handler) ?? (() => void 0);
  }
  afterFolderDeleted(handler) {
    return this.clientRef?.afterFolderDeleted(handler) ?? (() => void 0);
  }
  onChange(handler) {
    return this.clientRef?.onChange(handler) ?? (() => void 0);
  }
  /**
   * Live leaf as a Signal — re-computes when treeEpoch bumps.
   * Java get style; use in templates: `theme()`.
   *
   * @example
   * theme = this.kip.value('ui/theme', { defaultValue: 'dark' });
   */
  value(keyOrPath, options = {}) {
    const { defaultValue, folders } = options;
    return computed(() => {
      this.treeEpoch();
      this.status();
      if (!this.clientRef) return defaultValue;
      if (folders) {
        return this.clientRef.get(keyOrPath, defaultValue, ...folders);
      }
      if (keyOrPath.includes("/") || keyOrPath.includes(".")) {
        return this.clientRef.getPath(keyOrPath, defaultValue);
      }
      return this.clientRef.get(keyOrPath, defaultValue);
    });
  }
  /**
   * Live integer Signal (Java getInt).
   */
  valueInt(keyOrPath, defaultValue = 0, folders) {
    const raw = this.value(keyOrPath, {
      defaultValue: String(defaultValue),
      folders
    });
    return computed(() => {
      const v = raw();
      if (v === void 0 || v === "") return defaultValue;
      const n = parseInt(v, 10);
      return Number.isFinite(n) ? n : defaultValue;
    });
  }
};
KiponosService = __decorateClass([
  Injectable(),
  __decorateParam(0, Optional()),
  __decorateParam(0, Inject(KIPONOS_CLIENT)),
  __decorateParam(1, Optional()),
  __decorateParam(1, Inject(KIPONOS_CONFIG))
], KiponosService);
function provideKiponos(config = {}) {
  const providers = [
    { provide: KIPONOS_CONFIG, useValue: config },
    {
      provide: KIPONOS_CLIENT,
      useValue: config.client ?? null
    },
    KiponosService
  ];
  return makeEnvironmentProviders(providers);
}
function injectKiponos() {
  return inject(KiponosService);
}

export { KIPONOS_CLIENT, KIPONOS_CONFIG, KiponosService, injectKiponos, provideKiponos };
//# sourceMappingURL=index.js.map
//# sourceMappingURL=index.js.map