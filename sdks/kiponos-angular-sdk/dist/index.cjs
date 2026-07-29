'use strict';

var fs = require('fs');
var os = require('os');
var path = require('path');
var pako = require('pako');
var core = require('@angular/core');
var rxjs = require('rxjs');

var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __decorateClass = (decorators, target, key, kind) => {
  var result = kind > 1 ? void 0 : kind ? __getOwnPropDesc(target, key) : target;
  for (var i = decorators.length - 1, decorator; i >= 0; i--)
    if (decorator = decorators[i])
      result = (decorator(result)) || result;
  return result;
};
var __decorateParam = (index, decorator) => (target, key) => decorator(target, key, index);
var DEFAULT_PROFILE = "['Family-Agent']['1.0.0']['Alef-Dev']['base']";
function isBrowserRuntime() {
  return typeof globalThis !== "undefined" && typeof globalThis.document !== "undefined";
}
function isNodeRuntime() {
  return typeof process !== "undefined" && typeof process.versions === "object" && !!process.versions?.node;
}
function isValidBracketProfile(p) {
  return /\['[^']+'\]\['[^']+'\]\['[^']+'\]\['[^']+'\]/.test(p || "");
}
function parseDotEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return {};
  const out = {};
  for (const line of fs.readFileSync(filePath, "utf8").split("\n")) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const eq = t.indexOf("=");
    if (eq < 1) continue;
    const k = t.slice(0, eq).trim();
    let v = t.slice(eq + 1).trim();
    if (v.startsWith('"') && v.endsWith('"') || v.startsWith("'") && v.endsWith("'")) {
      v = v.slice(1, -1);
    }
    out[k] = v;
  }
  return out;
}
function loadEnvFileIntoProcess(filePath) {
  const parsed = parseDotEnvFile(filePath);
  const fileHasTokens = Boolean(parsed.KIPONOS_ID && parsed.KIPONOS_ACCESS);
  for (const [k, v] of Object.entries(parsed)) {
    const isProfileKey = k === "KIPONOS" || k === "KIPONOS_PROFILE";
    if (fileHasTokens && isProfileKey) {
      process.env[k] = v;
      continue;
    }
    if (fileHasTokens && (k === "KIPONOS_ID" || k === "KIPONOS_ACCESS")) {
      process.env[k] = v;
      continue;
    }
    if (process.env[k] === void 0 || process.env[k] === "") {
      process.env[k] = v;
    }
  }
}
function resolveEnvFilePath(explicit) {
  const fromOpt = explicit || process.env.KIPONOS_ENV_FILE || "";
  if (fromOpt && fs.existsSync(fromOpt)) return fromOpt;
  const defaultOtp = path.join(os.homedir(), ".config/kiponos/otp-listener.env");
  if (fs.existsSync(defaultOtp)) return defaultOtp;
  return void 0;
}
function resolveCredentialsFromEnv(options = {}) {
  if (isBrowserRuntime() || !isNodeRuntime()) {
    throw new Error(
      "[@kiponos/angular] createFromEnv is Node/server-only. Browser SPAs must not hold Connect tokens \u2014 use a Node BFF/SSE backend that calls Kiponos.createFromEnv()."
    );
  }
  const envFile = resolveEnvFilePath(options.envFile);
  if (envFile) {
    loadEnvFileIntoProcess(envFile);
  }
  const idToken = (process.env.KIPONOS_ID || "").trim();
  const accessToken = (process.env.KIPONOS_ACCESS || "").trim();
  if (!idToken || !accessToken) {
    throw new Error(
      "[@kiponos/angular] Missing KIPONOS_ID and/or KIPONOS_ACCESS in process environment. Set them like the Java SDK (export or systemd EnvironmentFile). Optional: KIPONOS_ENV_FILE pointing at a dotenv file."
    );
  }
  const candidates = [
    process.env.KIPONOS,
    process.env.KIPONOS_PROFILE,
    DEFAULT_PROFILE
  ];
  let profile = "";
  for (const c of candidates) {
    if (c && isValidBracketProfile(c)) {
      profile = c;
      break;
    }
  }
  if (!profile) {
    throw new Error(
      "[@kiponos/angular] Invalid or missing KIPONOS profile. Expected ['App']['Release']['Env']['Config'], e.g. " + DEFAULT_PROFILE
    );
  }
  const serverUrl = options.serverUrl || process.env.KIPONOS_SERVER || process.env.KIPONOS_WS || "wss://kiponos.io/api/io-kiponos-sdk";
  return { idToken, accessToken, profile, serverUrl };
}

// src/core/paths.ts
function profileToBasePath(profile) {
  const parts = [...(profile || "").matchAll(/\['([^']*)'\]/g)].map((m) => m[1]);
  if (parts.length < 4) {
    throw new Error(
      "Invalid Kiponos profile. Expected ['AppName']['Release']['Env']['ConfigName'], got: " + JSON.stringify(profile)
    );
  }
  const [app, rel, env, cfg] = parts;
  return `$.rootAccount['apps']['${app}']['rels']['${rel}']['envs']['${env}']['cfgs']['${cfg}']`;
}
function joinPath(base, ...folders) {
  let path = base;
  for (const f of folders) {
    if (!f) continue;
    const safe = f.replace(/'/g, "\\'");
    path = `${path}['${safe}']`;
  }
  return path;
}
function parseDottedOrSlash(path) {
  const p = (path || "").trim().replace(/^\/+|\/+$/g, "");
  if (!p) return [];
  if (p.includes("/")) return p.split("/").filter(Boolean);
  if (p.includes(".")) return p.split(".").filter(Boolean);
  return [p];
}
function foldersFromJsonPath(rel) {
  if (!rel) return [];
  return [...rel.matchAll(/\['([^']*)'\]/g)].map((m) => m[1]);
}
function foldersFromBase(basePath, eventBase) {
  if (!eventBase) return [];
  let rel = eventBase;
  if (rel.startsWith(basePath)) {
    rel = rel.slice(basePath.length);
  }
  return foldersFromJsonPath(rel);
}

// src/core/ws-factory.ts
var WS_OPEN = 1;
async function createSdkWebSocket(url, headers) {
  if (!headers && typeof globalThis.WebSocket !== "undefined") {
    const ws = new globalThis.WebSocket(url);
    ws.binaryType = "arraybuffer";
    return ws;
  }
  try {
    const mod = await import('ws');
    const Ctor = mod.default ?? mod.WebSocket ?? mod;
    if (typeof Ctor !== "function") {
      throw new Error(`ws package did not export a constructor: ${typeof Ctor}`);
    }
    const raw = new Ctor(url, headers ? { headers } : void 0);
    const adapter = {
      get readyState() {
        return raw.readyState;
      },
      binaryType: "arraybuffer",
      send(data) {
        raw.send(data);
      },
      close() {
        raw.close();
      },
      onopen: null,
      onmessage: null,
      onerror: null,
      onclose: null
    };
    raw.on("unexpected-response", (...args) => {
      const res = args[1];
      const status = res?.statusCode ?? 0;
      const err = new Error(
        `WebSocket handshake rejected HTTP ${status} (check tokens/profile match)`
      );
      adapter.onerror?.(err);
      try {
        raw.close();
      } catch {
      }
    });
    raw.on("open", () => adapter.onopen?.());
    raw.on("message", (...args) => {
      const data = args[0];
      let payload = data;
      if (data instanceof ArrayBuffer) {
        payload = data;
      } else if (ArrayBuffer.isView(data)) {
        const view = data;
        payload = view.buffer.slice(
          view.byteOffset,
          view.byteOffset + view.byteLength
        );
      } else if (typeof data === "string") {
        payload = data;
      }
      adapter.onmessage?.({ data: payload });
    });
    raw.on("error", (...args) => {
      const e = args[0];
      if (e instanceof Error) adapter.onerror?.(e);
      else adapter.onerror?.(new Error(String(e ?? "WebSocket error")));
    });
    raw.on("close", () => adapter.onclose?.());
    return adapter;
  } catch (e) {
    if (typeof globalThis.WebSocket !== "undefined") {
      const ws = new globalThis.WebSocket(url);
      ws.binaryType = "arraybuffer";
      return ws;
    }
    throw new Error(
      `Cannot create WebSocket (need browser WebSocket or npm package "ws"). ${e}`
    );
  }
}

// src/core/stomp.ts
function log(quiet, msg) {
  if (!quiet) console.log(`[kiponos-stomp] ${msg}`);
}
function parseTextFrame(raw) {
  if (!raw || raw === "\n" || raw === "\r\n" || raw === "\0") return null;
  const headerEnd = raw.indexOf("\n\n");
  if (headerEnd === -1) return null;
  const headerStr = raw.slice(0, headerEnd);
  let body = raw.slice(headerEnd + 2);
  if (body.endsWith("\0")) body = body.slice(0, -1);
  const lines = headerStr.split("\n");
  const command = lines[0].trim();
  const headers = {};
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim() || !line.includes(":")) continue;
    const idx = line.indexOf(":");
    headers[line.slice(0, idx).trim()] = line.slice(idx + 1).trim();
  }
  return { command, headers, body };
}
function normalizeIncoming(data) {
  if (data == null) return null;
  if (typeof data === "string") return data;
  let u8 = null;
  if (data instanceof ArrayBuffer) {
    u8 = new Uint8Array(data);
  } else if (ArrayBuffer.isView(data)) {
    const v = data;
    u8 = new Uint8Array(v.buffer, v.byteOffset, v.byteLength);
  }
  if (!u8 || u8.length === 0) return null;
  if (u8.length <= 2 && (u8[0] === 10 || u8[0] === 0 || u8[0] === 13)) {
    return "\n";
  }
  const peekLen = Math.min(u8.length, 48);
  const peek = new TextDecoder("utf-8").decode(u8.subarray(0, peekLen));
  if (peek.startsWith("CONNECTED") || peek.startsWith("ERROR") || peek.startsWith("RECEIPT")) {
    return new TextDecoder("utf-8").decode(u8);
  }
  const copy = new Uint8Array(u8.byteLength);
  copy.set(u8);
  return copy.buffer;
}
function parseBinaryFrame(data) {
  const bytes = data instanceof Uint8Array ? data : data instanceof ArrayBuffer ? new Uint8Array(data) : null;
  if (!bytes || bytes.length === 0) return null;
  if (bytes.length === 1 && (bytes[0] === 10 || bytes[0] === 0)) return null;
  let headerEnd = -1;
  for (let i = 0; i < bytes.length - 1; i++) {
    if (bytes[i] === 10 && bytes[i + 1] === 10) {
      headerEnd = i;
      break;
    }
  }
  if (headerEnd === -1) return null;
  const headerBytes = bytes.slice(0, headerEnd);
  let body = bytes.slice(headerEnd + 2);
  if (body.length && body[body.length - 1] === 0) {
    body = body.slice(0, -1);
  }
  const headerStr = new TextDecoder("utf-8").decode(headerBytes);
  const lines = headerStr.split("\n");
  const command = lines[0].trim();
  const headers = {};
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim() || !line.includes(":")) continue;
    const idx = line.indexOf(":");
    headers[line.slice(0, idx).trim()] = line.slice(idx + 1).trim();
  }
  return { command, headers, body };
}
var StompSocket = class {
  constructor(opts) {
    this.ws = null;
    this.hbTimer = null;
    this.hbSendMs = 0;
    this.connected = false;
    this.opts = opts;
  }
  async connect() {
    const url = this.opts.url;
    const ws = await createSdkWebSocket(url, this.opts.headers);
    this.ws = ws;
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error("WebSocket connection timeout"));
        try {
          ws.close();
        } catch {
        }
      }, 25e3);
      let settled = false;
      ws.onopen = () => {
        this.opts.onOpen?.();
        const host = this.opts.host || (() => {
          try {
            return new URL(url).hostname;
          } catch {
            return "kiponos.io";
          }
        })();
        const [cx, cy] = this.opts.heartBeatMs || [1e4, 1e4];
        try {
          this.sendRaw(
            `CONNECT
accept-version:1.2
host:${host}
heart-beat:${cx},${cy}

\0`
          );
        } catch (e) {
          clearTimeout(timeout);
          if (!settled) {
            settled = true;
            reject(e instanceof Error ? e : new Error(String(e)));
          }
        }
      };
      ws.onerror = (ev) => {
        clearTimeout(timeout);
        const err = ev instanceof Error ? ev : new Error(
          typeof ev === "object" && ev && "message" in ev && typeof ev.message === "string" ? ev.message : "WebSocket error during connect"
        );
        this.opts.onError?.(err);
        if (!settled) {
          settled = true;
          reject(err);
        }
      };
      ws.onclose = () => {
        this.connected = false;
        this.stopHeartbeat();
        this.opts.onClose?.();
        if (!settled) {
          settled = true;
          clearTimeout(timeout);
          reject(new Error("WebSocket closed before STOMP CONNECTED"));
        }
      };
      ws.onmessage = (ev) => {
        if (settled) {
          this.dispatchData(ev.data);
          return;
        }
        const normalized = normalizeIncoming(ev.data);
        if (normalized == null) return;
        if (typeof normalized === "string") {
          if (normalized.startsWith("CONNECTED")) {
            this.applyConnectedHeartbeat(normalized);
            this.connected = true;
            settled = true;
            clearTimeout(timeout);
            resolve();
            return;
          }
          if (normalized.startsWith("ERROR")) {
            settled = true;
            clearTimeout(timeout);
            reject(new Error(`STOMP ERROR: ${normalized.slice(0, 300)}`));
            return;
          }
          if (normalized === "\n" || normalized === "\r\n") return;
        }
      };
    });
    if (this.ws) {
      this.ws.onmessage = (ev) => this.dispatchData(ev.data);
      this.ws.onerror = (ev) => {
        const err = ev instanceof Error ? ev : new Error("WebSocket error");
        this.opts.onError?.(err);
      };
    }
    this.startHeartbeat();
  }
  applyConnectedHeartbeat(connectedFrame) {
    const headers = {};
    for (const line of connectedFrame.split("\n").slice(1)) {
      if (!line.includes(":")) continue;
      const idx = line.indexOf(":");
      headers[line.slice(0, idx).trim().toLowerCase()] = line.slice(idx + 1).trim();
    }
    const hb = headers["heart-beat"] || headers["heartbeat"] || "0,0";
    let sx = 0;
    try {
      const [a] = hb.split(",");
      sx = parseInt((a || "0").trim(), 10) || 0;
    } catch {
      sx = 0;
    }
    if (sx > 0) {
      this.hbSendMs = Math.max(1e3, Math.floor(sx * 0.5));
    } else {
      const [cx] = this.opts.heartBeatMs || [1e4, 1e4];
      this.hbSendMs = cx > 0 ? Math.max(1e3, Math.floor(cx * 0.5)) : 0;
    }
    log(
      this.opts.quiet,
      `heart-beat negotiated server=${hb} client_send_ms=${this.hbSendMs}`
    );
  }
  startHeartbeat() {
    this.stopHeartbeat();
    if (this.hbSendMs <= 0) return;
    setTimeout(() => {
      if (this.connected && this.ws?.readyState === WS_OPEN) {
        try {
          this.ws.send("\n");
        } catch {
        }
      }
    }, 2e3);
    this.hbTimer = setInterval(() => {
      if (!this.connected || !this.ws || this.ws.readyState !== WS_OPEN) {
        return;
      }
      try {
        this.ws.send("\n");
      } catch {
      }
    }, this.hbSendMs);
  }
  stopHeartbeat() {
    if (this.hbTimer) {
      clearInterval(this.hbTimer);
      this.hbTimer = null;
    }
  }
  dispatchData(data) {
    const normalized = normalizeIncoming(data);
    if (normalized == null) return;
    if (typeof normalized === "string") {
      if (normalized === "\n" || normalized === "\r\n" || normalized === "\0") {
        return;
      }
      const frame2 = parseTextFrame(normalized);
      if (frame2) this.opts.onMessage(frame2);
      return;
    }
    const frame = parseBinaryFrame(normalized);
    if (frame) this.opts.onMessage(frame);
  }
  sendRaw(data) {
    if (!this.ws || this.ws.readyState !== WS_OPEN) {
      throw new Error("STOMP socket not open");
    }
    this.ws.send(data);
  }
  sendJson(destination, payload) {
    const body = JSON.stringify(payload);
    const frame = `SEND
destination:${destination}
content-type:application/json
content-length:${new TextEncoder().encode(body).length}

${body}\0`;
    this.sendRaw(frame);
  }
  subscribe(id, destination) {
    this.sendRaw(`SUBSCRIBE
id:${id}
destination:${destination}

\0`);
  }
  disconnect() {
    this.connected = false;
    this.stopHeartbeat();
    try {
      if (this.ws && this.ws.readyState === WS_OPEN) {
        this.sendRaw("DISCONNECT\n\n\0");
        this.ws.close();
      }
    } catch {
    }
    this.ws = null;
  }
};
function buildBrowserSdkUrl(serverUrl, tokens) {
  const u = new URL(serverUrl);
  u.searchParams.set("sdk-id-token", tokens.idToken);
  u.searchParams.set("sdk-access-token", tokens.accessToken);
  u.searchParams.set("kiponos-id", tokens.profile);
  u.searchParams.set("sdk-version", tokens.sdkVersion);
  return u.toString();
}
function buildSdkAuthHeaders(tokens) {
  return {
    "sdk-id-token": tokens.idToken,
    "sdk-access-token": tokens.accessToken,
    "kiponos-id": tokens.profile,
    "sdk-version": tokens.sdkVersion
  };
}

// src/core/tree.ts
var VALUE_KEY = "value";
function isKeyNode(node) {
  return typeof node === "object" && node !== null && VALUE_KEY in node && Object.keys(node).length === 1;
}
function resolveNode(tree, folders) {
  let node = tree;
  for (const f of folders) {
    if (typeof node !== "object" || node === null || !(f in node)) {
      throw new Error(`Folder not found: ${folders.join("/")} (at ${JSON.stringify(f)})`);
    }
    const next = node[f];
    if (isKeyNode(next)) {
      throw new Error(`Path hits a key, not a folder: ${f}`);
    }
    node = next;
  }
  if (typeof node !== "object" || node === null) {
    throw new Error(`Not a folder: ${folders.join("/")}`);
  }
  return node;
}
function ensureLocalFolders(tree, folders) {
  let parent = tree;
  for (const f of folders) {
    const cur = parent[f];
    if (cur === void 0 || isKeyNode(cur)) {
      parent[f] = {};
    }
    parent = parent[f];
  }
  return parent;
}
function applyValueAt(tree, folders, key, value) {
  const parent = folders.length === 0 ? tree : ensureLocalFolders(tree, folders);
  parent[key] = { [VALUE_KEY]: value };
}
function getValueAt(tree, folders, key, defaultValue) {
  try {
    const parent = folders.length ? resolveNode(tree, folders) : tree;
    const node = parent[key];
    if (isKeyNode(node)) return node.value;
    return defaultValue;
  } catch {
    return defaultValue;
  }
}
function listKeysAt(tree, folders) {
  const parent = folders.length ? resolveNode(tree, folders) : tree;
  return Object.keys(parent).filter((k) => isKeyNode(parent[k])).sort();
}
function listFoldersAt(tree, folders) {
  const parent = folders.length ? resolveNode(tree, folders) : tree;
  return Object.keys(parent).filter((k) => {
    const n = parent[k];
    return typeof n === "object" && n !== null && !isKeyNode(n);
  }).sort();
}
function deepCloneTree(tree) {
  return JSON.parse(JSON.stringify(tree));
}

// src/core/kiponos-client.ts
var DEFAULT_SERVER = "wss://kiponos.io/api/io-kiponos-sdk";
var DEFAULT_SDK_VERSION = "0.1.0-angular";
var Q_BOOTSTRAP = "/user/queue/sdk-boot";
var KiponosClient = class {
  /**
   * @internal Use Kiponos.createFromEnv() / createForCurrentTeam() instead.
   */
  constructor(options) {
    this.stomp = null;
    this.configTree = {};
    this.teamIdInternal = "";
    this.statusInternal = "idle";
    this.errorInternal = null;
    this.subSeq = 200;
    this.handlersBySub = /* @__PURE__ */ new Map();
    this.pending = /* @__PURE__ */ new Map();
    this.listeners = {
      valueUpdated: /* @__PURE__ */ new Set(),
      keyCreated: /* @__PURE__ */ new Set(),
      keyDeleted: /* @__PURE__ */ new Set(),
      keyRenamed: /* @__PURE__ */ new Set(),
      itemSaved: /* @__PURE__ */ new Set(),
      folderCreated: /* @__PURE__ */ new Set(),
      folderDeleted: /* @__PURE__ */ new Set(),
      change: /* @__PURE__ */ new Set(),
      tree: /* @__PURE__ */ new Set(),
      status: /* @__PURE__ */ new Set()
    };
    this.connectPromise = null;
    this.readyWaiters = [];
    if (!options.__fromEnv) {
      throw new Error(
        "[@kiponos/angular] Do not construct KiponosClient with tokens. Use Kiponos.createFromEnv() or Kiponos.createForCurrentTeam() (reads KIPONOS_ID / KIPONOS_ACCESS / KIPONOS from process env \u2014 Java SDK parity)."
      );
    }
    if (!options.profile) throw new Error("profile is required");
    if (!options.idToken || !options.accessToken) {
      throw new Error("idToken and accessToken are required (from env)");
    }
    this.profile = options.profile;
    this.basePath = profileToBasePath(options.profile);
    const authMode = options.authMode || "headers";
    this.opts = {
      ...options,
      serverUrl: options.serverUrl || DEFAULT_SERVER,
      sdkVersion: options.sdkVersion || DEFAULT_SDK_VERSION,
      requestTimeoutMs: options.requestTimeoutMs ?? 3e4,
      quiet: options.quiet ?? true,
      authMode
    };
    if (options.autoConnect) {
      void this.connect().catch((e) => this.log(`autoConnect failed: ${e}`));
    }
  }
  get status() {
    return this.statusInternal;
  }
  get ready() {
    return this.statusInternal === "ready";
  }
  get error() {
    return this.errorInternal;
  }
  get teamId() {
    return this.teamIdInternal;
  }
  setStatus(s) {
    this.statusInternal = s;
    for (const h of this.listeners.status) {
      try {
        h(s);
      } catch {
      }
    }
  }
  log(msg) {
    if (!this.opts.quiet) console.log(`[kiponos] ${msg}`);
  }
  /** Run after the current STOMP handler stack (avoids set↔listener reentrancy). */
  defer(fn) {
    queueMicrotask(() => {
      try {
        fn();
      } catch (e) {
        this.log(`deferred handler error: ${e}`);
      }
    });
  }
  fireTree() {
    this.defer(() => {
      for (const h of this.listeners.tree) {
        try {
          h();
        } catch {
        }
      }
    });
  }
  emit(key, value, folders, source, delta) {
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
        }
      }
    });
  }
  onStatus(handler) {
    this.listeners.status.add(handler);
    return () => this.listeners.status.delete(handler);
  }
  onTreeChanged(handler) {
    this.listeners.tree.add(handler);
    return () => this.listeners.tree.delete(handler);
  }
  onChange(handler) {
    this.listeners.change.add(handler);
    return () => this.listeners.change.delete(handler);
  }
  afterValueUpdated(handler) {
    this.listeners.valueUpdated.add(handler);
    return () => this.listeners.valueUpdated.delete(handler);
  }
  afterKeyCreated(handler) {
    this.listeners.keyCreated.add(handler);
    return () => this.listeners.keyCreated.delete(handler);
  }
  afterKeyDeleted(handler) {
    this.listeners.keyDeleted.add(handler);
    return () => this.listeners.keyDeleted.delete(handler);
  }
  afterKeyRenamed(handler) {
    this.listeners.keyRenamed.add(handler);
    return () => this.listeners.keyRenamed.delete(handler);
  }
  afterItemSaved(handler) {
    this.listeners.itemSaved.add(handler);
    return () => this.listeners.itemSaved.delete(handler);
  }
  afterFolderCreated(handler) {
    this.listeners.folderCreated.add(handler);
    return () => this.listeners.folderCreated.delete(handler);
  }
  afterFolderDeleted(handler) {
    this.listeners.folderDeleted.add(handler);
    return () => this.listeners.folderDeleted.delete(handler);
  }
  waitUntilReady(timeoutMs = 3e4) {
    if (this.ready) return Promise.resolve();
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.readyWaiters = this.readyWaiters.filter((w) => w.resolve !== resolve);
        reject(new Error("waitUntilReady timeout"));
      }, timeoutMs);
      this.readyWaiters.push({ resolve, reject, timer });
    });
  }
  resolveReadyWaiters() {
    for (const w of this.readyWaiters) {
      clearTimeout(w.timer);
      w.resolve();
    }
    this.readyWaiters = [];
  }
  rejectReadyWaiters(err) {
    for (const w of this.readyWaiters) {
      clearTimeout(w.timer);
      w.reject(err);
    }
    this.readyWaiters = [];
  }
  async connect() {
    if (this.ready && this.stomp?.connected) return;
    if (this.connectPromise) return this.connectPromise;
    this.connectPromise = this.doConnect().finally(() => {
      this.connectPromise = null;
    });
    return this.connectPromise;
  }
  resolveAuth() {
    const tokens = {
      idToken: this.opts.idToken,
      accessToken: this.opts.accessToken,
      profile: this.profile,
      sdkVersion: this.opts.sdkVersion
    };
    const mode = this.opts.authMode;
    const inBrowser = typeof globalThis !== "undefined" && typeof globalThis.document !== "undefined";
    if (mode === "query" || mode === "auto" && inBrowser) {
      return { url: buildBrowserSdkUrl(this.opts.serverUrl, tokens) };
    }
    return {
      url: this.opts.serverUrl,
      headers: buildSdkAuthHeaders(tokens)
    };
  }
  async doConnect() {
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
    let bootstrapResolve;
    let bootstrapReject;
    const bootstrapPromise = new Promise((res, rej) => {
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
        if (this.statusInternal === "ready" || this.statusInternal === "connecting") {
          this.setStatus("disconnected");
        }
        this.failAllPending(new Error("WebSocket closed"));
      }
    });
    this.stomp = stomp;
    try {
      await stomp.connect();
      stomp.subscribe("0", Q_BOOTSTRAP);
      await Promise.race([
        bootstrapPromise,
        new Promise(
          (_, rej) => setTimeout(() => rej(new Error("Bootstrap timeout")), 3e4)
        )
      ]);
      if (!this.teamIdInternal) {
        throw new Error("Bootstrap missing teamId \u2014 cannot subscribe to team topics");
      }
      this.subsTopic("config-key-created", (d) => this.onKeyCreated(d));
      this.subsTopic("config-val-updated", (d) => this.onValUpdated(d));
      this.subsTopic("config-prop-saved", (d) => this.onPropSaved(d));
      this.subsTopic("config-folder-created", (d) => this.onFolderCreated(d));
      this.subsTopic("config-folder-deleted", (d) => this.onFolderDeleted(d));
      this.subsTopic("config-key-deleted", (d) => this.onKeyDeleted(d));
      this.subsTopic("config-key-renamed", (d) => this.onKeyRenamed(d));
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
      }
      this.stomp = null;
      throw err;
    }
  }
  handleBootstrap(frame) {
    let bodyBytes;
    if (frame.body instanceof Uint8Array) {
      bodyBytes = frame.body;
    } else if (typeof frame.body === "string") {
      throw new Error("Expected binary bootstrap MESSAGE");
    } else {
      throw new Error("Empty bootstrap body");
    }
    let decompressed;
    try {
      decompressed = pako.inflateRaw(bodyBytes);
    } catch {
      try {
        decompressed = pako.inflate(bodyBytes);
      } catch (e2) {
        throw new Error(`Bootstrap inflate failed: ${e2}`);
      }
    }
    const text = new TextDecoder("utf-8").decode(decompressed);
    const jsonArr = JSON.parse(text);
    this.configTree = jsonArr[0] && typeof jsonArr[0] === "object" ? jsonArr[0] : {};
    const teamInfo = jsonArr[1] || {};
    this.teamIdInternal = teamInfo.teamId || "";
    this.log(
      `bootstrap keys\u2248${Object.keys(this.configTree).length} team=${this.teamIdInternal}`
    );
  }
  subsTopic(topicName, handler) {
    if (!this.stomp) throw new Error("Not connected");
    this.subSeq += 1;
    const id = String(this.subSeq);
    const dest = `/topic/team/${this.teamIdInternal}/${topicName}`;
    this.handlersBySub.set(id, handler);
    this.stomp.subscribe(id, dest);
    this.log(`Subscribed ${id}: ${dest}`);
  }
  handleFrame(frame) {
    if (frame.command === "ERROR") {
      this.log(`STOMP ERROR: ${String(frame.body).slice(0, 200)}`);
      return;
    }
    if (frame.command !== "MESSAGE") return;
    const subId = frame.headers["subscription"] || "";
    const handler = this.handlersBySub.get(subId);
    if (!handler) return;
    let bodyStr;
    if (typeof frame.body === "string") {
      bodyStr = frame.body;
    } else {
      bodyStr = new TextDecoder("utf-8").decode(frame.body);
    }
    let delta = {};
    try {
      delta = bodyStr ? JSON.parse(bodyStr) : {};
    } catch {
      return;
    }
    try {
      handler(delta);
    } catch (e) {
      this.log(`handler error: ${e}`);
    }
  }
  onKeyCreated(delta) {
    const key = String(delta.key || "");
    if (!key) return;
    const folders = foldersFromBase(this.basePath, delta.basePath);
    applyValueAt(this.configTree, folders, key, "");
    const ev = delta;
    this.defer(() => {
      for (const h of this.listeners.keyCreated) {
        try {
          h(ev);
        } catch {
        }
      }
    });
    this.emit(key, "", folders, "config-key-created", delta);
  }
  onValUpdated(delta) {
    const key = delta.key != null ? String(delta.key) : "";
    if (!key) return;
    const value = delta.value != null ? String(delta.value) : "";
    const folders = foldersFromBase(this.basePath, delta.basePath);
    applyValueAt(this.configTree, folders, key, value);
    const ev = delta;
    this.defer(() => {
      for (const h of this.listeners.valueUpdated) {
        try {
          h(ev);
        } catch {
        }
      }
    });
    this.emit(key, value, folders, "config-val-updated", delta);
  }
  onPropSaved(delta) {
    const reqId = delta.requestId;
    const key = delta.key != null ? String(delta.key) : "";
    const value = delta.value != null ? String(delta.value) : "";
    const base = delta.basePath || "";
    if (key) {
      const folders = foldersFromBase(this.basePath, base);
      try {
        applyValueAt(this.configTree, folders, key, value);
      } catch {
        applyValueAt(this.configTree, folders, key, value);
      }
    }
    if (reqId) this.completePending(reqId, value);
    if (key) {
      const folders = foldersFromBase(this.basePath, base);
      const ev = delta;
      this.defer(() => {
        for (const h of this.listeners.itemSaved) {
          try {
            h(ev);
          } catch {
          }
        }
      });
      this.emit(key, value, folders, "config-prop-saved", delta);
    }
  }
  onFolderCreated(delta) {
    const reqId = delta.requestId;
    const folder = String(delta.folder || delta.folderName || "");
    const path = delta.path || delta.basePath || "";
    if (folder) {
      const folders = foldersFromBase(this.basePath, path);
      try {
        const parent = folders.length === 0 ? this.configTree : ensureLocalFolders(this.configTree, folders);
        if (!(folder in parent) || isKeyNode(parent[folder])) {
          parent[folder] = {};
        }
      } catch {
      }
    }
    if (reqId) this.completePending(reqId, folder);
    const ev = delta;
    for (const h of this.listeners.folderCreated) {
      try {
        h(ev);
      } catch {
      }
    }
    this.fireTree();
  }
  onFolderDeleted(delta) {
    const reqId = delta.requestId;
    const folder = String(delta.folderName || delta.folder || "");
    const path = delta.basePath || delta.path || "";
    if (folder) {
      const folders = foldersFromBase(this.basePath, path);
      try {
        const parent = folders.length === 0 ? this.configTree : resolveNode(this.configTree, folders);
        delete parent[folder];
      } catch {
      }
    }
    if (reqId) this.completePending(reqId, folder);
    const ev = delta;
    for (const h of this.listeners.folderDeleted) {
      try {
        h(ev);
      } catch {
      }
    }
    this.fireTree();
  }
  onKeyDeleted(delta) {
    const reqId = delta.requestId;
    const key = delta.key != null ? String(delta.key) : "";
    const base = delta.basePath || "";
    if (key) {
      const folders = foldersFromBase(this.basePath, base);
      try {
        const parent = folders.length === 0 ? this.configTree : resolveNode(this.configTree, folders);
        delete parent[key];
      } catch {
      }
    }
    if (reqId) this.completePending(reqId, key);
    if (key) {
      const folders = foldersFromBase(this.basePath, base);
      const ev = delta;
      for (const h of this.listeners.keyDeleted) {
        try {
          h(ev);
        } catch {
        }
      }
      this.emit(key, void 0, folders, "config-key-deleted", delta);
    }
  }
  onKeyRenamed(delta) {
    const oldKey = String(delta.oldKey || delta.key || "");
    const newKey = String(delta.newKey || "");
    const base = delta.basePath || "";
    const folders = foldersFromBase(this.basePath, base);
    if (oldKey && newKey) {
      try {
        const parent = folders.length === 0 ? this.configTree : resolveNode(this.configTree, folders);
        const node = parent[oldKey];
        if (node !== void 0) {
          parent[newKey] = node;
          delete parent[oldKey];
        }
      } catch {
      }
    }
    const ev = delta;
    for (const h of this.listeners.keyRenamed) {
      try {
        h(ev);
      } catch {
      }
    }
    this.emit(newKey || oldKey, void 0, folders, "config-key-renamed", delta);
  }
  newRequestId() {
    return `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
  }
  wait(requestId, timeoutMs) {
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
  async waitOr(requestId, fallback, timeoutMs) {
    try {
      return await this.wait(requestId, timeoutMs);
    } catch (e) {
      this.log(
        `ack timeout ${requestId}: ${e instanceof Error ? e.message : e} \u2014 using optimistic local apply`
      );
      return fallback;
    }
  }
  completePending(requestId, value) {
    const id = String(requestId);
    const p = this.pending.get(id);
    if (!p) return;
    clearTimeout(p.timer);
    this.pending.delete(id);
    p.resolve(value);
  }
  failAllPending(err) {
    for (const [id, p] of this.pending) {
      clearTimeout(p.timer);
      this.pending.delete(id);
      p.reject(err);
    }
  }
  ensureConnected() {
    if (!this.stomp?.connected || !this.ready) {
      throw new Error("KiponosClient not ready \u2014 await connect() / waitUntilReady()");
    }
  }
  get(key, defaultValue, ...folders) {
    return getValueAt(this.configTree, folders, key, defaultValue);
  }
  getPath(path, defaultValue) {
    const parts = parseDottedOrSlash(path);
    if (!parts.length) return defaultValue;
    return this.get(parts[parts.length - 1], defaultValue, ...parts.slice(0, -1));
  }
  async set(key, value, ...folders) {
    this.ensureConnected();
    if (!key) throw new Error("key required");
    const str = value === null || value === void 0 ? "" : String(value);
    const base = joinPath(this.basePath, ...folders);
    let oldKey = null;
    try {
      const parent = folders.length === 0 ? this.configTree : resolveNode(this.configTree, folders);
      const exists = key in parent && isKeyNode(parent[key]);
      oldKey = exists ? key : null;
      if (exists && parent[key].value === str) {
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
      oldKey
    };
    this.stomp.sendJson("/app/sdk-save-config-prop", payload);
    const result = await this.waitOr(requestId, str);
    applyValueAt(this.configTree, folders, key, str);
    this.fireTree();
    return result != null ? String(result) : str;
  }
  async setPath(path, value) {
    const parts = parseDottedOrSlash(path);
    if (!parts.length) throw new Error("empty path");
    return this.set(parts[parts.length - 1], value, ...parts.slice(0, -1));
  }
  async mkdir(folderName, ...parentFolders) {
    this.ensureConnected();
    if (!folderName) throw new Error("folder_name required");
    const parentPath = joinPath(this.basePath, ...parentFolders);
    try {
      const parent = parentFolders.length === 0 ? this.configTree : resolveNode(this.configTree, parentFolders);
      if (folderName in parent && !isKeyNode(parent[folderName])) {
        return folderName;
      }
    } catch {
    }
    const requestId = this.newRequestId();
    this.stomp.sendJson("/app/sdk-create-config-folder", {
      requestId,
      path: parentPath,
      folder: folderName
    });
    const result = await this.waitOr(
      requestId,
      folderName,
      Math.min(this.opts.requestTimeoutMs, 12e3)
    );
    ensureLocalFolders(this.configTree, [...parentFolders, folderName]);
    this.fireTree();
    return result != null ? String(result) : folderName;
  }
  async ensurePath(...folders) {
    const built = [];
    for (const f of folders) {
      const parent = [...built];
      try {
        const pnode = parent.length === 0 ? this.configTree : resolveNode(this.configTree, parent);
        if (f in pnode && !isKeyNode(pnode[f])) {
          built.push(f);
          continue;
        }
      } catch {
      }
      await this.mkdir(f, ...parent);
      built.push(f);
    }
  }
  async deleteKey(key, ...folders) {
    this.ensureConnected();
    if (!key) throw new Error("key required");
    const base = joinPath(this.basePath, ...folders);
    try {
      const parent = folders.length === 0 ? this.configTree : resolveNode(this.configTree, folders);
      if (!(key in parent) || !isKeyNode(parent[key])) return key;
    } catch {
      return key;
    }
    const requestId = this.newRequestId();
    this.stomp.sendJson("/app/delete-config-key", {
      requestId,
      basePath: base,
      key
    });
    try {
      await this.wait(requestId, Math.min(this.opts.requestTimeoutMs, 2e4));
    } catch {
      this.stomp.sendJson("/app/delete-config-prop", {
        basePath: base,
        key
      });
      await new Promise((r) => setTimeout(r, 400));
    }
    try {
      const parent = folders.length === 0 ? this.configTree : resolveNode(this.configTree, folders);
      delete parent[key];
    } catch {
    }
    this.fireTree();
    return key;
  }
  async deleteFolder(folderName, ...parentFolders) {
    this.ensureConnected();
    if (!folderName) throw new Error("folder_name required");
    const parentPath = joinPath(this.basePath, ...parentFolders);
    try {
      const parent = parentFolders.length === 0 ? this.configTree : resolveNode(this.configTree, parentFolders);
      if (!(folderName in parent) || isKeyNode(parent[folderName])) {
        return folderName;
      }
    } catch {
      return folderName;
    }
    const requestId = this.newRequestId();
    this.stomp.sendJson("/app/sdk-delete-config-folder", {
      requestId,
      basePath: parentPath,
      folderName
    });
    await this.wait(requestId);
    try {
      const parent = parentFolders.length === 0 ? this.configTree : resolveNode(this.configTree, parentFolders);
      delete parent[folderName];
    } catch {
    }
    this.fireTree();
    return folderName;
  }
  listKeys(...folders) {
    try {
      return listKeysAt(this.configTree, folders);
    } catch {
      return [];
    }
  }
  listFolders(...folders) {
    try {
      return listFoldersAt(this.configTree, folders);
    } catch {
      return [];
    }
  }
  dump(...folders) {
    if (!folders.length) return deepCloneTree(this.configTree);
    try {
      return deepCloneTree(resolveNode(this.configTree, folders));
    } catch {
      return {};
    }
  }
  path(...folders) {
    return this.makeFolder(folders);
  }
  makeFolder(folders) {
    const client = this;
    return {
      get folders() {
        return folders;
      },
      path(...more) {
        return client.makeFolder([...folders, ...more]);
      },
      folder(name) {
        return client.makeFolder([...folders, name]);
      },
      async folderOrCreate(name) {
        await client.mkdir(name, ...folders);
        return client.makeFolder([...folders, name]);
      },
      get(key, defaultValue) {
        return client.get(key, defaultValue, ...folders);
      },
      getInt(key, defaultValue = 0) {
        const v = client.get(key, void 0, ...folders);
        if (v === void 0 || v === "") return defaultValue;
        const n = parseInt(v, 10);
        return Number.isFinite(n) ? n : defaultValue;
      },
      getLong(key, defaultValue = 0) {
        return this.getInt(key, defaultValue);
      },
      set(key, value) {
        return client.set(key, value, ...folders);
      },
      hasKey(key) {
        try {
          const parent = folders.length === 0 ? client.configTree : resolveNode(client.configTree, folders);
          return key in parent && isKeyNode(parent[key]);
        } catch {
          return false;
        }
      },
      hasFolder(name) {
        try {
          const parent = folders.length === 0 ? client.configTree : resolveNode(client.configTree, folders);
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
      }
    };
  }
  disconnect() {
    this.failAllPending(new Error("disconnected"));
    try {
      this.stomp?.disconnect();
    } catch {
    }
    this.stomp = null;
    this.handlersBySub.clear();
    this.setStatus("disconnected");
  }
};

// src/core/kiponos.ts
var singleton = null;
function buildFromEnv(options = {}) {
  const creds = resolveCredentialsFromEnv(options);
  return new KiponosClient({
    __fromEnv: true,
    profile: creds.profile,
    idToken: creds.idToken,
    accessToken: creds.accessToken,
    serverUrl: creds.serverUrl,
    quiet: options.quiet ?? true,
    autoConnect: options.autoConnect ?? false,
    requestTimeoutMs: options.requestTimeoutMs,
    sdkVersion: options.sdkVersion,
    authMode: "headers"
  });
}
var Kiponos = class _Kiponos {
  /**
   * Create a client from process environment (KIPONOS_ID, KIPONOS_ACCESS, KIPONOS).
   * Optional dotenv via options.envFile or KIPONOS_ENV_FILE.
   * Same idea as constructing after reading System.getenv in Java.
   */
  static createFromEnv(options = {}) {
    return buildFromEnv(options);
  }
  /**
   * Java-style singleton for the current process / team connection.
   * First call creates; later calls return the same instance.
   */
  static createForCurrentTeam(options = {}) {
    if (!singleton) {
      singleton = buildFromEnv(options);
    }
    return singleton;
  }
  /** Alias for createFromEnv. */
  static fromEnv(options = {}) {
    return _Kiponos.createFromEnv(options);
  }
  /** Drop singleton (tests / graceful shutdown). */
  static resetSingleton() {
    if (singleton) {
      try {
        singleton.disconnect();
      } catch {
      }
    }
    singleton = null;
  }
};
var KIPONOS_CLIENT = new core.InjectionToken(
  "KIPONOS_CLIENT"
);
var KIPONOS_CONFIG = new core.InjectionToken(
  "KIPONOS_CONFIG"
);

// src/angular/kiponos.service.ts
exports.KiponosService = class KiponosService {
  constructor(externalClient, config) {
    this.destroyRef = core.inject(core.DestroyRef);
    /** Connection status (signal). */
    this.status = core.signal("idle");
    /** Last error (signal). */
    this.error = core.signal(null);
    /** True when status === 'ready'. */
    this.ready = core.computed(() => this.status() === "ready");
    /** Tree mutation epoch — bump on any local/remote tree change. */
    this.treeEpoch = core.signal(0);
    this.status$ = new rxjs.Subject();
    this.change$ = new rxjs.Subject();
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
    return core.computed(() => {
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
    return core.computed(() => {
      const v = raw();
      if (v === void 0 || v === "") return defaultValue;
      const n = parseInt(v, 10);
      return Number.isFinite(n) ? n : defaultValue;
    });
  }
};
exports.KiponosService = __decorateClass([
  core.Injectable(),
  __decorateParam(0, core.Optional()),
  __decorateParam(0, core.Inject(KIPONOS_CLIENT)),
  __decorateParam(1, core.Optional()),
  __decorateParam(1, core.Inject(KIPONOS_CONFIG))
], exports.KiponosService);
function provideKiponos(config = {}) {
  const providers = [
    { provide: KIPONOS_CONFIG, useValue: config },
    {
      provide: KIPONOS_CLIENT,
      useValue: config.client ?? null
    },
    exports.KiponosService
  ];
  return core.makeEnvironmentProviders(providers);
}
function injectKiponos() {
  return core.inject(exports.KiponosService);
}

exports.DEFAULT_PROFILE = DEFAULT_PROFILE;
exports.KIPONOS_CLIENT = KIPONOS_CLIENT;
exports.KIPONOS_CONFIG = KIPONOS_CONFIG;
exports.Kiponos = Kiponos;
exports.KiponosClient = KiponosClient;
exports.VALUE_KEY = VALUE_KEY;
exports.applyValueAt = applyValueAt;
exports.buildBrowserSdkUrl = buildBrowserSdkUrl;
exports.buildSdkAuthHeaders = buildSdkAuthHeaders;
exports.foldersFromBase = foldersFromBase;
exports.getValueAt = getValueAt;
exports.injectKiponos = injectKiponos;
exports.isKeyNode = isKeyNode;
exports.isValidBracketProfile = isValidBracketProfile;
exports.joinPath = joinPath;
exports.listFoldersAt = listFoldersAt;
exports.listKeysAt = listKeysAt;
exports.parseDottedOrSlash = parseDottedOrSlash;
exports.profileToBasePath = profileToBasePath;
exports.provideKiponos = provideKiponos;
exports.resolveCredentialsFromEnv = resolveCredentialsFromEnv;
//# sourceMappingURL=index.cjs.map
//# sourceMappingURL=index.cjs.map