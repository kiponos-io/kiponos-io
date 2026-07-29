import { inflate, inflateRaw } from "pako";
import {
  foldersFromBase,
  joinPath,
  parseDottedOrSlash,
  profileToBasePath,
} from "./paths";
import {
  buildBrowserSdkUrl,
  buildSdkAuthHeaders,
  StompSocket,
  type StompFrame,
} from "./stomp";
import {
  applyValueAt,
  deepCloneTree,
  getValueAt,
  isKeyNode,
  listFoldersAt,
  listKeysAt,
  resolveNode,
  ensureLocalFolders,
} from "./tree";
import type {
  ChangeSource,
  ConfigFolderCreatedEvent,
  ConfigFolderDeletedEvent,
  ConfigItemSavedEvent,
  ConfigKeyCreatedEvent,
  ConfigKeyDeletedEvent,
  ConfigKeyRenamedEvent,
  ConfigTree,
  ConfigValUpdatedEvent,
  KiponosClientOptions,
  KiponosClientPublic,
  KiponosFolder,
  KiponosStatus,
  OnChangeHandler,
  Unsubscribe,
} from "./types";

const DEFAULT_SERVER = "wss://kiponos.io/api/io-kiponos-sdk";
const DEFAULT_SDK_VERSION = "0.1.0-angular";
const Q_BOOTSTRAP = "/user/queue/sdk-boot";

type ListenerMap = {
  valueUpdated: Set<(e: ConfigValUpdatedEvent) => void>;
  keyCreated: Set<(e: ConfigKeyCreatedEvent) => void>;
  keyDeleted: Set<(e: ConfigKeyDeletedEvent) => void>;
  keyRenamed: Set<(e: ConfigKeyRenamedEvent) => void>;
  itemSaved: Set<(e: ConfigItemSavedEvent) => void>;
  folderCreated: Set<(e: ConfigFolderCreatedEvent) => void>;
  folderDeleted: Set<(e: ConfigFolderDeletedEvent) => void>;
  change: Set<OnChangeHandler>;
  tree: Set<() => void>;
  status: Set<(s: KiponosStatus) => void>;
};

/**
 * Framework-agnostic Kiponos real-time client (Java/Python SDK parity).
 */
export class KiponosClient implements KiponosClientPublic {
  readonly profile: string;
  readonly basePath: string;

  private readonly opts: Required<
    Pick<
      KiponosClientOptions,
      | "serverUrl"
      | "sdkVersion"
      | "requestTimeoutMs"
      | "quiet"
      | "authMode"
    >
  > &
    KiponosClientOptions;

  private stomp: StompSocket | null = null;
  private configTree: ConfigTree = {};
  private teamIdInternal = "";
  private statusInternal: KiponosStatus = "idle";
  private errorInternal: Error | null = null;
  private subSeq = 200;
  private readonly handlersBySub = new Map<string, (delta: Record<string, unknown>) => void>();
  private readonly pending = new Map<
    string,
    { resolve: (v: unknown) => void; reject: (e: Error) => void; timer: ReturnType<typeof setTimeout> }
  >();
  private readonly listeners: ListenerMap = {
    valueUpdated: new Set(),
    keyCreated: new Set(),
    keyDeleted: new Set(),
    keyRenamed: new Set(),
    itemSaved: new Set(),
    folderCreated: new Set(),
    folderDeleted: new Set(),
    change: new Set(),
    tree: new Set(),
    status: new Set(),
  };
  private connectPromise: Promise<void> | null = null;
  private readyWaiters: Array<{
    resolve: () => void;
    reject: (e: Error) => void;
    timer: ReturnType<typeof setTimeout>;
  }> = [];

  /**
   * @internal Use Kiponos.createFromEnv() / createForCurrentTeam() instead.
   */
  constructor(options: KiponosClientOptions) {
    if (!options.__fromEnv) {
      throw new Error(
        "[@kiponos/angular] Do not construct KiponosClient with tokens. " +
          "Use Kiponos.createFromEnv() or Kiponos.createForCurrentTeam() " +
          "(reads KIPONOS_ID / KIPONOS_ACCESS / KIPONOS from process env — Java SDK parity)."
      );
    }
    if (!options.profile) throw new Error("profile is required");
    if (!options.idToken || !options.accessToken) {
      throw new Error("idToken and accessToken are required (from env)");
    }
    this.profile = options.profile;
    this.basePath = profileToBasePath(options.profile);
    // Server participant: always HTTP headers (not browser query)
    const authMode: "headers" | "query" | "auto" = options.authMode || "headers";
    this.opts = {
      ...options,
      serverUrl: options.serverUrl || DEFAULT_SERVER,
      sdkVersion: options.sdkVersion || DEFAULT_SDK_VERSION,
      requestTimeoutMs: options.requestTimeoutMs ?? 30000,
      quiet: options.quiet ?? true,
      authMode,
    };
    if (options.autoConnect) {
      void this.connect().catch((e) => this.log(`autoConnect failed: ${e}`));
    }
  }

  get status(): KiponosStatus {
    return this.statusInternal;
  }

  get ready(): boolean {
    return this.statusInternal === "ready";
  }

  get error(): Error | null {
    return this.errorInternal;
  }

  get teamId(): string {
    return this.teamIdInternal;
  }

  private setStatus(s: KiponosStatus): void {
    this.statusInternal = s;
    for (const h of this.listeners.status) {
      try {
        h(s);
      } catch {
        /* ignore */
      }
    }
  }

  private log(msg: string): void {
    if (!this.opts.quiet) console.log(`[kiponos] ${msg}`);
  }

  /** Run after the current STOMP handler stack (avoids set↔listener reentrancy). */
  private defer(fn: () => void): void {
    queueMicrotask(() => {
      try {
        fn();
      } catch (e) {
        this.log(`deferred handler error: ${e}`);
      }
    });
  }

  private fireTree(): void {
    this.defer(() => {
      for (const h of this.listeners.tree) {
        try {
          h();
        } catch {
          /* ignore */
        }
      }
    });
  }

  private emit(
    key: string,
    value: string | undefined,
    folders: string[],
    source: ChangeSource,
    delta: Record<string, unknown>
  ): void {
    // Never invoke user onChange/tree hooks on the STOMP/set ack stack —
    // apps that set() from onChange would otherwise deadlock (JS single-thread:
    // waitOr cannot complete while the MESSAGE handler is still nested).
    this.defer(() => {
      for (const cb of this.listeners.change) {
        try {
          cb(key, value, folders, source, delta);
        } catch (e) {
          this.log(`onChange error: ${e}`);
        }
      }
      for (const h of this.listeners.tree) {
        try {
          h();
        } catch {
          /* ignore */
        }
      }
    });
  }

  onStatus(handler: (s: KiponosStatus) => void): Unsubscribe {
    this.listeners.status.add(handler);
    return () => this.listeners.status.delete(handler);
  }

  onTreeChanged(handler: () => void): Unsubscribe {
    this.listeners.tree.add(handler);
    return () => this.listeners.tree.delete(handler);
  }

  onChange(handler: OnChangeHandler): Unsubscribe {
    this.listeners.change.add(handler);
    return () => this.listeners.change.delete(handler);
  }

  afterValueUpdated(handler: (e: ConfigValUpdatedEvent) => void): Unsubscribe {
    this.listeners.valueUpdated.add(handler);
    return () => this.listeners.valueUpdated.delete(handler);
  }

  afterKeyCreated(handler: (e: ConfigKeyCreatedEvent) => void): Unsubscribe {
    this.listeners.keyCreated.add(handler);
    return () => this.listeners.keyCreated.delete(handler);
  }

  afterKeyDeleted(handler: (e: ConfigKeyDeletedEvent) => void): Unsubscribe {
    this.listeners.keyDeleted.add(handler);
    return () => this.listeners.keyDeleted.delete(handler);
  }

  afterKeyRenamed(handler: (e: ConfigKeyRenamedEvent) => void): Unsubscribe {
    this.listeners.keyRenamed.add(handler);
    return () => this.listeners.keyRenamed.delete(handler);
  }

  afterItemSaved(handler: (e: ConfigItemSavedEvent) => void): Unsubscribe {
    this.listeners.itemSaved.add(handler);
    return () => this.listeners.itemSaved.delete(handler);
  }

  afterFolderCreated(
    handler: (e: ConfigFolderCreatedEvent) => void
  ): Unsubscribe {
    this.listeners.folderCreated.add(handler);
    return () => this.listeners.folderCreated.delete(handler);
  }

  afterFolderDeleted(
    handler: (e: ConfigFolderDeletedEvent) => void
  ): Unsubscribe {
    this.listeners.folderDeleted.add(handler);
    return () => this.listeners.folderDeleted.delete(handler);
  }

  waitUntilReady(timeoutMs = 30000): Promise<void> {
    if (this.ready) return Promise.resolve();
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.readyWaiters = this.readyWaiters.filter((w) => w.resolve !== resolve);
        reject(new Error("waitUntilReady timeout"));
      }, timeoutMs);
      this.readyWaiters.push({ resolve, reject, timer });
    });
  }

  private resolveReadyWaiters(): void {
    for (const w of this.readyWaiters) {
      clearTimeout(w.timer);
      w.resolve();
    }
    this.readyWaiters = [];
  }

  private rejectReadyWaiters(err: Error): void {
    for (const w of this.readyWaiters) {
      clearTimeout(w.timer);
      w.reject(err);
    }
    this.readyWaiters = [];
  }

  async connect(): Promise<void> {
    if (this.ready && this.stomp?.connected) return;
    if (this.connectPromise) return this.connectPromise;

    this.connectPromise = this.doConnect().finally(() => {
      this.connectPromise = null;
    });
    return this.connectPromise;
  }

  private resolveAuth(): {
    url: string;
    headers?: ReturnType<typeof buildSdkAuthHeaders>;
  } {
    const tokens = {
      idToken: this.opts.idToken,
      accessToken: this.opts.accessToken,
      profile: this.profile,
      sdkVersion: this.opts.sdkVersion,
    };
    const mode = this.opts.authMode;
    const inBrowser =
      typeof globalThis !== "undefined" &&
      typeof (globalThis as { document?: unknown }).document !== "undefined";

    if (mode === "query" || (mode === "auto" && inBrowser)) {
      return { url: buildBrowserSdkUrl(this.opts.serverUrl, tokens) };
    }
    // headers / Node auto
    return {
      url: this.opts.serverUrl,
      headers: buildSdkAuthHeaders(tokens),
    };
  }

  private async doConnect(): Promise<void> {
    this.setStatus("connecting");
    this.errorInternal = null;

    const { url, headers } = this.resolveAuth();
    this.log(
      `connecting auth=${headers ? "headers" : "query"} host=${(() => {
        try {
          return new URL(url).host;
        } catch {
          return "?";
        }
      })()}`
    );

    let bootstrapDone = false;
    let bootstrapResolve!: () => void;
    let bootstrapReject!: (e: Error) => void;
    const bootstrapPromise = new Promise<void>((res, rej) => {
      bootstrapResolve = res;
      bootstrapReject = rej;
    });

    const stomp = new StompSocket({
      url,
      headers,
      quiet: this.opts.quiet,
      onMessage: (frame) => {
        if (!bootstrapDone && frame.command === "MESSAGE") {
          const dest = frame.headers["destination"] || "";
          if (dest.includes("sdk-boot") || frame.body instanceof Uint8Array) {
            try {
              this.handleBootstrap(frame);
              bootstrapDone = true;
              bootstrapResolve();
            } catch (e) {
              bootstrapReject(e instanceof Error ? e : new Error(String(e)));
            }
            return;
          }
        }
        this.handleFrame(frame);
      },
      onError: (err) => {
        this.errorInternal = err;
        this.setStatus("error");
        this.failAllPending(err);
        this.log(`socket error: ${err.message}`);
      },
      onClose: () => {
        if (
          this.statusInternal === "ready" ||
          this.statusInternal === "connecting"
        ) {
          this.setStatus("disconnected");
        }
        this.failAllPending(new Error("WebSocket closed"));
      },
    });

    this.stomp = stomp;

    try {
      await stomp.connect();
      stomp.subscribe("0", Q_BOOTSTRAP);
      await Promise.race([
        bootstrapPromise,
        new Promise<void>((_, rej) =>
          setTimeout(() => rej(new Error("Bootstrap timeout")), 30000)
        ),
      ]);

      if (!this.teamIdInternal) {
        throw new Error("Bootstrap missing teamId — cannot subscribe to team topics");
      }

      this.subsTopic("config-key-created", (d) => this.onKeyCreated(d));
      this.subsTopic("config-val-updated", (d) => this.onValUpdated(d));
      this.subsTopic("config-prop-saved", (d) => this.onPropSaved(d));
      this.subsTopic("config-folder-created", (d) => this.onFolderCreated(d));
      this.subsTopic("config-folder-deleted", (d) => this.onFolderDeleted(d));
      this.subsTopic("config-key-deleted", (d) => this.onKeyDeleted(d));
      this.subsTopic("config-key-renamed", (d) => this.onKeyRenamed(d));

      // brief settle for subscriptions
      await new Promise((r) => setTimeout(r, 350));

      this.setStatus("ready");
      this.resolveReadyWaiters();
      this.fireTree();
      this.log(`Connected team=${this.teamIdInternal} profile=${this.profile}`);
    } catch (e) {
      const err = e instanceof Error ? e : new Error(String(e));
      this.errorInternal = err;
      this.setStatus("error");
      this.rejectReadyWaiters(err);
      try {
        stomp.disconnect();
      } catch {
        /* ignore */
      }
      this.stomp = null;
      throw err;
    }
  }

  private handleBootstrap(frame: StompFrame): void {
    let bodyBytes: Uint8Array;
    if (frame.body instanceof Uint8Array) {
      bodyBytes = frame.body;
    } else if (typeof frame.body === "string") {
      throw new Error("Expected binary bootstrap MESSAGE");
    } else {
      throw new Error("Empty bootstrap body");
    }

    // Python: zlib.decompress(body, wbits=-MAX_WBITS) → raw deflate
    let decompressed: Uint8Array;
    try {
      decompressed = inflateRaw(bodyBytes);
    } catch {
      try {
        decompressed = inflate(bodyBytes);
      } catch (e2) {
        throw new Error(`Bootstrap inflate failed: ${e2}`);
      }
    }
    const text = new TextDecoder("utf-8").decode(decompressed);
    const jsonArr = JSON.parse(text) as unknown[];
    this.configTree =
      (jsonArr[0] as ConfigTree) && typeof jsonArr[0] === "object"
        ? (jsonArr[0] as ConfigTree)
        : {};
    const teamInfo = (jsonArr[1] as { teamId?: string }) || {};
    this.teamIdInternal = teamInfo.teamId || "";
    this.log(
      `bootstrap keys≈${Object.keys(this.configTree).length} team=${this.teamIdInternal}`
    );
  }

  private subsTopic(
    topicName: string,
    handler: (delta: Record<string, unknown>) => void
  ): void {
    if (!this.stomp) throw new Error("Not connected");
    this.subSeq += 1;
    const id = String(this.subSeq);
    const dest = `/topic/team/${this.teamIdInternal}/${topicName}`;
    this.handlersBySub.set(id, handler);
    this.stomp.subscribe(id, dest);
    this.log(`Subscribed ${id}: ${dest}`);
  }

  private handleFrame(frame: StompFrame): void {
    if (frame.command === "ERROR") {
      this.log(`STOMP ERROR: ${String(frame.body).slice(0, 200)}`);
      return;
    }
    if (frame.command !== "MESSAGE") return;

    const subId = frame.headers["subscription"] || "";
    const handler = this.handlersBySub.get(subId);
    if (!handler) return;

    let bodyStr: string;
    if (typeof frame.body === "string") {
      bodyStr = frame.body;
    } else {
      bodyStr = new TextDecoder("utf-8").decode(frame.body);
    }
    let delta: Record<string, unknown> = {};
    try {
      delta = bodyStr ? (JSON.parse(bodyStr) as Record<string, unknown>) : {};
    } catch {
      return;
    }
    try {
      handler(delta);
    } catch (e) {
      this.log(`handler error: ${e}`);
    }
  }

  private onKeyCreated(delta: Record<string, unknown>): void {
    const key = String(delta.key || "");
    if (!key) return;
    const folders = foldersFromBase(this.basePath, delta.basePath as string | undefined);
    applyValueAt(this.configTree, folders, key, "");
    const ev = delta as ConfigKeyCreatedEvent;
    this.defer(() => {
      for (const h of this.listeners.keyCreated) {
        try {
          h(ev);
        } catch {
          /* ignore */
        }
      }
    });
    this.emit(key, "", folders, "config-key-created", delta);
  }

  private onValUpdated(delta: Record<string, unknown>): void {
    const key = delta.key != null ? String(delta.key) : "";
    if (!key) return;
    const value = delta.value != null ? String(delta.value) : "";
    const folders = foldersFromBase(this.basePath, delta.basePath as string | undefined);
    applyValueAt(this.configTree, folders, key, value);
    const ev = delta as ConfigValUpdatedEvent;
    this.defer(() => {
      for (const h of this.listeners.valueUpdated) {
        try {
          h(ev);
        } catch {
          /* ignore */
        }
      }
    });
    this.emit(key, value, folders, "config-val-updated", delta);
  }

  private onPropSaved(delta: Record<string, unknown>): void {
    const reqId = delta.requestId as string | undefined;
    const key = delta.key != null ? String(delta.key) : "";
    const value = delta.value != null ? String(delta.value) : "";
    const base = (delta.basePath as string) || "";
    if (key) {
      const folders = foldersFromBase(this.basePath, base);
      try {
        applyValueAt(this.configTree, folders, key, value);
      } catch {
        applyValueAt(this.configTree, folders, key, value);
      }
    }
    // Resolve waiters BEFORE any deferred user hooks
    if (reqId) this.completePending(reqId, value);
    if (key) {
      const folders = foldersFromBase(this.basePath, base);
      const ev = delta as ConfigItemSavedEvent;
      this.defer(() => {
        for (const h of this.listeners.itemSaved) {
          try {
            h(ev);
          } catch {
            /* ignore */
          }
        }
      });
      this.emit(key, value, folders, "config-prop-saved", delta);
    }
  }

  private onFolderCreated(delta: Record<string, unknown>): void {
    const reqId = delta.requestId as string | undefined;
    const folder = String(delta.folder || delta.folderName || "");
    const path = (delta.path || delta.basePath || "") as string;
    if (folder) {
      const folders = foldersFromBase(this.basePath, path);
      try {
        const parent =
          folders.length === 0
            ? this.configTree
            : ensureLocalFolders(this.configTree, folders);
        if (!(folder in parent) || isKeyNode(parent[folder])) {
          parent[folder] = {};
        }
      } catch {
        /* ignore */
      }
    }
    if (reqId) this.completePending(reqId, folder);
    const ev = delta as ConfigFolderCreatedEvent;
    for (const h of this.listeners.folderCreated) {
      try {
        h(ev);
      } catch {
        /* ignore */
      }
    }
    this.fireTree();
  }

  private onFolderDeleted(delta: Record<string, unknown>): void {
    const reqId = delta.requestId as string | undefined;
    const folder = String(delta.folderName || delta.folder || "");
    const path = (delta.basePath || delta.path || "") as string;
    if (folder) {
      const folders = foldersFromBase(this.basePath, path);
      try {
        const parent =
          folders.length === 0
            ? this.configTree
            : resolveNode(this.configTree, folders);
        delete parent[folder];
      } catch {
        /* ignore */
      }
    }
    if (reqId) this.completePending(reqId, folder);
    const ev = delta as ConfigFolderDeletedEvent;
    for (const h of this.listeners.folderDeleted) {
      try {
        h(ev);
      } catch {
        /* ignore */
      }
    }
    this.fireTree();
  }

  private onKeyDeleted(delta: Record<string, unknown>): void {
    const reqId = delta.requestId as string | undefined;
    const key = delta.key != null ? String(delta.key) : "";
    const base = (delta.basePath as string) || "";
    if (key) {
      const folders = foldersFromBase(this.basePath, base);
      try {
        const parent =
          folders.length === 0
            ? this.configTree
            : resolveNode(this.configTree, folders);
        delete parent[key];
      } catch {
        /* ignore */
      }
    }
    if (reqId) this.completePending(reqId, key);
    if (key) {
      const folders = foldersFromBase(this.basePath, base);
      const ev = delta as ConfigKeyDeletedEvent;
      for (const h of this.listeners.keyDeleted) {
        try {
          h(ev);
        } catch {
          /* ignore */
        }
      }
      this.emit(key, undefined, folders, "config-key-deleted", delta);
    }
  }

  private onKeyRenamed(delta: Record<string, unknown>): void {
    const oldKey = String(delta.oldKey || delta.key || "");
    const newKey = String(delta.newKey || "");
    const base = (delta.basePath as string) || "";
    const folders = foldersFromBase(this.basePath, base);
    if (oldKey && newKey) {
      try {
        const parent =
          folders.length === 0
            ? this.configTree
            : resolveNode(this.configTree, folders);
        const node = parent[oldKey];
        if (node !== undefined) {
          parent[newKey] = node;
          delete parent[oldKey];
        }
      } catch {
        /* ignore */
      }
    }
    const ev = delta as ConfigKeyRenamedEvent;
    for (const h of this.listeners.keyRenamed) {
      try {
        h(ev);
      } catch {
        /* ignore */
      }
    }
    this.emit(newKey || oldKey, undefined, folders, "config-key-renamed", delta);
  }

  private newRequestId(): string {
    return `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
  }

  private wait(requestId: string, timeoutMs?: number): Promise<unknown> {
    const ms = timeoutMs ?? this.opts.requestTimeoutMs;
    const id = String(requestId);
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error(`No server response for request ${id} within timeout`));
      }, ms);
      this.pending.set(id, { resolve, reject, timer });
    });
  }

  /**
   * Wait for ack; on timeout return `fallback` instead of throwing.
   * Needed when server skips broadcasts for already-existing folders/keys.
   */
  private async waitOr(
    requestId: string,
    fallback: unknown,
    timeoutMs?: number
  ): Promise<unknown> {
    try {
      return await this.wait(requestId, timeoutMs);
    } catch (e) {
      this.log(
        `ack timeout ${requestId}: ${e instanceof Error ? e.message : e} — using optimistic local apply`
      );
      return fallback;
    }
  }

  private completePending(requestId: string, value: unknown): void {
    const id = String(requestId);
    const p = this.pending.get(id);
    if (!p) return;
    clearTimeout(p.timer);
    this.pending.delete(id);
    p.resolve(value);
  }

  private failAllPending(err: Error): void {
    for (const [id, p] of this.pending) {
      clearTimeout(p.timer);
      this.pending.delete(id);
      p.reject(err);
    }
  }

  private ensureConnected(): void {
    if (!this.stomp?.connected || !this.ready) {
      throw new Error("KiponosClient not ready — await connect() / waitUntilReady()");
    }
  }

  get(
    key: string,
    defaultValue?: string,
    ...folders: string[]
  ): string | undefined {
    return getValueAt(this.configTree, folders, key, defaultValue);
  }

  getPath(path: string, defaultValue?: string): string | undefined {
    const parts = parseDottedOrSlash(path);
    if (!parts.length) return defaultValue;
    return this.get(parts[parts.length - 1], defaultValue, ...parts.slice(0, -1));
  }

  async set(
    key: string,
    value: string | number | boolean,
    ...folders: string[]
  ): Promise<string> {
    this.ensureConnected();
    if (!key) throw new Error("key required");
    const str = value === null || value === undefined ? "" : String(value);
    const base = joinPath(this.basePath, ...folders);

    let oldKey: string | null = null;
    try {
      const parent =
        folders.length === 0
          ? this.configTree
          : resolveNode(this.configTree, folders);
      const exists = key in parent && isKeyNode(parent[key]);
      oldKey = exists ? key : null;
      if (exists && (parent[key] as { value: string }).value === str) {
        return str;
      }
    } catch {
      oldKey = null;
    }

    const requestId = this.newRequestId();
    const payload = {
      requestId,
      basePath: base,
      key,
      value: str,
      oldKey,
    };
    this.stomp!.sendJson("/app/sdk-save-config-prop", payload);
    const result = await this.waitOr(requestId, str);
    // Optimistic local apply so multi-participant races stay usable
    applyValueAt(this.configTree, folders, key, str);
    this.fireTree();
    return result != null ? String(result) : str;
  }

  async setPath(
    path: string,
    value: string | number | boolean
  ): Promise<string> {
    const parts = parseDottedOrSlash(path);
    if (!parts.length) throw new Error("empty path");
    return this.set(parts[parts.length - 1], value, ...parts.slice(0, -1));
  }

  async mkdir(folderName: string, ...parentFolders: string[]): Promise<string> {
    this.ensureConnected();
    if (!folderName) throw new Error("folder_name required");
    const parentPath = joinPath(this.basePath, ...parentFolders);

    try {
      const parent =
        parentFolders.length === 0
          ? this.configTree
          : resolveNode(this.configTree, parentFolders);
      if (folderName in parent && !isKeyNode(parent[folderName])) {
        return folderName;
      }
    } catch {
      /* create */
    }

    const requestId = this.newRequestId();
    this.stomp!.sendJson("/app/sdk-create-config-folder", {
      requestId,
      path: parentPath,
      folder: folderName,
    });
    // Server may omit team broadcast when folder already exists → timeout is OK
    const result = await this.waitOr(
      requestId,
      folderName,
      Math.min(this.opts.requestTimeoutMs, 12000)
    );
    ensureLocalFolders(this.configTree, [...parentFolders, folderName]);
    this.fireTree();
    return result != null ? String(result) : folderName;
  }

  async ensurePath(...folders: string[]): Promise<void> {
    const built: string[] = [];
    for (const f of folders) {
      const parent = [...built];
      try {
        const pnode =
          parent.length === 0
            ? this.configTree
            : resolveNode(this.configTree, parent);
        if (f in pnode && !isKeyNode(pnode[f])) {
          built.push(f);
          continue;
        }
      } catch {
        /* create */
      }
      await this.mkdir(f, ...parent);
      built.push(f);
    }
  }

  async deleteKey(key: string, ...folders: string[]): Promise<string> {
    this.ensureConnected();
    if (!key) throw new Error("key required");
    const base = joinPath(this.basePath, ...folders);
    try {
      const parent =
        folders.length === 0
          ? this.configTree
          : resolveNode(this.configTree, folders);
      if (!(key in parent) || !isKeyNode(parent[key])) return key;
    } catch {
      return key;
    }

    const requestId = this.newRequestId();
    this.stomp!.sendJson("/app/delete-config-key", {
      requestId,
      basePath: base,
      key,
    });
    try {
      await this.wait(requestId, Math.min(this.opts.requestTimeoutMs, 20000));
    } catch {
      this.stomp!.sendJson("/app/delete-config-prop", {
        basePath: base,
        key,
      });
      await new Promise((r) => setTimeout(r, 400));
    }
    try {
      const parent =
        folders.length === 0
          ? this.configTree
          : resolveNode(this.configTree, folders);
      delete parent[key];
    } catch {
      /* ignore */
    }
    this.fireTree();
    return key;
  }

  async deleteFolder(
    folderName: string,
    ...parentFolders: string[]
  ): Promise<string | null> {
    this.ensureConnected();
    if (!folderName) throw new Error("folder_name required");
    const parentPath = joinPath(this.basePath, ...parentFolders);
    try {
      const parent =
        parentFolders.length === 0
          ? this.configTree
          : resolveNode(this.configTree, parentFolders);
      if (!(folderName in parent) || isKeyNode(parent[folderName])) {
        return folderName;
      }
    } catch {
      return folderName;
    }

    const requestId = this.newRequestId();
    this.stomp!.sendJson("/app/sdk-delete-config-folder", {
      requestId,
      basePath: parentPath,
      folderName,
    });
    await this.wait(requestId);
    try {
      const parent =
        parentFolders.length === 0
          ? this.configTree
          : resolveNode(this.configTree, parentFolders);
      delete parent[folderName];
    } catch {
      /* ignore */
    }
    this.fireTree();
    return folderName;
  }

  listKeys(...folders: string[]): string[] {
    try {
      return listKeysAt(this.configTree, folders);
    } catch {
      return [];
    }
  }

  listFolders(...folders: string[]): string[] {
    try {
      return listFoldersAt(this.configTree, folders);
    } catch {
      return [];
    }
  }

  dump(...folders: string[]): ConfigTree {
    if (!folders.length) return deepCloneTree(this.configTree);
    try {
      return deepCloneTree(resolveNode(this.configTree, folders));
    } catch {
      return {};
    }
  }

  path(...folders: string[]): KiponosFolder {
    return this.makeFolder(folders);
  }

  private makeFolder(folders: string[]): KiponosFolder {
    const client = this;
    return {
      get folders() {
        return folders;
      },
      path(...more: string[]) {
        return client.makeFolder([...folders, ...more]);
      },
      folder(name: string) {
        return client.makeFolder([...folders, name]);
      },
      async folderOrCreate(name: string) {
        await client.mkdir(name, ...folders);
        return client.makeFolder([...folders, name]);
      },
      get(key: string, defaultValue?: string) {
        return client.get(key, defaultValue, ...folders);
      },
      getInt(key: string, defaultValue = 0) {
        const v = client.get(key, undefined, ...folders);
        if (v === undefined || v === "") return defaultValue;
        const n = parseInt(v, 10);
        return Number.isFinite(n) ? n : defaultValue;
      },
      getLong(key: string, defaultValue = 0) {
        return this.getInt(key, defaultValue);
      },
      set(key: string, value: string | number | boolean) {
        return client.set(key, value, ...folders);
      },
      hasKey(key: string) {
        try {
          const parent =
            folders.length === 0
              ? client.configTree
              : resolveNode(client.configTree, folders);
          return key in parent && isKeyNode(parent[key]);
        } catch {
          return false;
        }
      },
      hasFolder(name: string) {
        try {
          const parent =
            folders.length === 0
              ? client.configTree
              : resolveNode(client.configTree, folders);
          const n = parent[name];
          return typeof n === "object" && n !== null && !isKeyNode(n);
        } catch {
          return false;
        }
      },
      listKeys() {
        return client.listKeys(...folders);
      },
      listFolders() {
        return client.listFolders(...folders);
      },
      dump() {
        return client.dump(...folders);
      },
    };
  }

  disconnect(): void {
    this.failAllPending(new Error("disconnected"));
    try {
      this.stomp?.disconnect();
    } catch {
      /* ignore */
    }
    this.stomp = null;
    this.handlersBySub.clear();
    this.setStatus("disconnected");
  }
}
