/**
 * Minimal STOMP 1.2 over browser/Node WebSocket.
 * Heartbeats + binary bootstrap frames (aligned with Python agent_client).
 */

import {
  createSdkWebSocket,
  WS_OPEN,
  type SdkAuthHeaders,
  type SdkWebSocket,
} from "./ws-factory";

export type StompMessageHandler = (frame: StompFrame) => void;

export interface StompFrame {
  command: string;
  headers: Record<string, string>;
  body: string | Uint8Array;
}

export interface StompConnectOptions {
  url: string;
  /** HTTP upgrade headers (Node / Java-parity). */
  headers?: SdkAuthHeaders;
  host?: string;
  heartBeatMs?: [number, number];
  onMessage: StompMessageHandler;
  onError?: (err: Error) => void;
  onClose?: () => void;
  onOpen?: () => void;
  quiet?: boolean;
}

function log(quiet: boolean | undefined, msg: string): void {
  if (!quiet) console.log(`[kiponos-stomp] ${msg}`);
}

function parseTextFrame(raw: string): StompFrame | null {
  if (!raw || raw === "\n" || raw === "\r\n" || raw === "\0") return null;
  const headerEnd = raw.indexOf("\n\n");
  if (headerEnd === -1) return null;
  const headerStr = raw.slice(0, headerEnd);
  let body = raw.slice(headerEnd + 2);
  if (body.endsWith("\0")) body = body.slice(0, -1);
  const lines = headerStr.split("\n");
  const command = lines[0].trim();
  const headers: Record<string, string> = {};
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim() || !line.includes(":")) continue;
    const idx = line.indexOf(":");
    headers[line.slice(0, idx).trim()] = line.slice(idx + 1).trim();
  }
  return { command, headers, body };
}

/**
 * Node `ws` delivers text frames as Buffer. CONNECTED/ERROR are text;
 * bootstrap MESSAGE is binary. Coerce accordingly.
 */
function normalizeIncoming(data: unknown): string | ArrayBuffer | null {
  if (data == null) return null;
  if (typeof data === "string") return data;
  let u8: Uint8Array | null = null;
  if (data instanceof ArrayBuffer) {
    u8 = new Uint8Array(data);
  } else if (ArrayBuffer.isView(data)) {
    const v = data as ArrayBufferView;
    u8 = new Uint8Array(v.buffer, v.byteOffset, v.byteLength);
  }
  if (!u8 || u8.length === 0) return null;
  // heartbeat
  if (u8.length <= 2 && (u8[0] === 10 || u8[0] === 0 || u8[0] === 13)) {
    return "\n";
  }
  const peekLen = Math.min(u8.length, 48);
  const peek = new TextDecoder("utf-8").decode(u8.subarray(0, peekLen));
  if (
    peek.startsWith("CONNECTED") ||
    peek.startsWith("ERROR") ||
    peek.startsWith("RECEIPT")
  ) {
    return new TextDecoder("utf-8").decode(u8);
  }
  // MESSAGE or other → keep binary view (bootstrap body is compressed)
  // Copy into a real ArrayBuffer (avoid SharedArrayBuffer typing issues)
  const copy = new Uint8Array(u8.byteLength);
  copy.set(u8);
  return copy.buffer;
}

function parseBinaryFrame(data: ArrayBuffer | Uint8Array): StompFrame | null {
  const bytes =
    data instanceof Uint8Array
      ? data
      : data instanceof ArrayBuffer
        ? new Uint8Array(data)
        : null;
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
  const headers: Record<string, string> = {};
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim() || !line.includes(":")) continue;
    const idx = line.indexOf(":");
    headers[line.slice(0, idx).trim()] = line.slice(idx + 1).trim();
  }
  return { command, headers, body };
}

export class StompSocket {
  private ws: SdkWebSocket | null = null;
  private hbTimer: ReturnType<typeof setInterval> | null = null;
  private hbSendMs = 0;
  private readonly opts: StompConnectOptions;
  connected = false;

  constructor(opts: StompConnectOptions) {
    this.opts = opts;
  }

  async connect(): Promise<void> {
    const url = this.opts.url;
    const ws = await createSdkWebSocket(url, this.opts.headers);
    this.ws = ws;

    await new Promise<void>((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error("WebSocket connection timeout"));
        try {
          ws.close();
        } catch {
          /* ignore */
        }
      }, 25000);

      let settled = false;

      ws.onopen = () => {
        this.opts.onOpen?.();
        const host =
          this.opts.host ||
          (() => {
            try {
              return new URL(url).hostname;
            } catch {
              return "kiponos.io";
            }
          })();
        const [cx, cy] = this.opts.heartBeatMs || [10000, 10000];
        try {
          this.sendRaw(
            `CONNECT\naccept-version:1.2\nhost:${host}\nheart-beat:${cx},${cy}\n\n\0`
          );
        } catch (e) {
          clearTimeout(timeout);
          if (!settled) {
            settled = true;
            reject(e instanceof Error ? e : new Error(String(e)));
          }
        }
      };

      ws.onerror = (ev?: unknown) => {
        clearTimeout(timeout);
        const err =
          ev instanceof Error
            ? ev
            : new Error(
                typeof ev === "object" &&
                  ev &&
                  "message" in ev &&
                  typeof (ev as { message: unknown }).message === "string"
                  ? (ev as { message: string }).message
                  : "WebSocket error during connect"
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
        // binary before CONNECTED — ignore
      };
    });

    // Permanent handlers after CONNECTED
    if (this.ws) {
      this.ws.onmessage = (ev) => this.dispatchData(ev.data);
      this.ws.onerror = (ev?: unknown) => {
        const err =
          ev instanceof Error ? ev : new Error("WebSocket error");
        this.opts.onError?.(err);
      };
    }
    this.startHeartbeat();
  }

  private applyConnectedHeartbeat(connectedFrame: string): void {
    const headers: Record<string, string> = {};
    for (const line of connectedFrame.split("\n").slice(1)) {
      if (!line.includes(":")) continue;
      const idx = line.indexOf(":");
      headers[line.slice(0, idx).trim().toLowerCase()] = line
        .slice(idx + 1)
        .trim();
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
      this.hbSendMs = Math.max(1000, Math.floor(sx * 0.5));
    } else {
      const [cx] = this.opts.heartBeatMs || [10000, 10000];
      this.hbSendMs = cx > 0 ? Math.max(1000, Math.floor(cx * 0.5)) : 0;
    }
    log(
      this.opts.quiet,
      `heart-beat negotiated server=${hb} client_send_ms=${this.hbSendMs}`
    );
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    if (this.hbSendMs <= 0) return;
    setTimeout(() => {
      if (this.connected && this.ws?.readyState === WS_OPEN) {
        try {
          this.ws.send("\n");
        } catch {
          /* ignore */
        }
      }
    }, 2000);
    this.hbTimer = setInterval(() => {
      if (!this.connected || !this.ws || this.ws.readyState !== WS_OPEN) {
        return;
      }
      try {
        this.ws.send("\n");
      } catch {
        /* ignore */
      }
    }, this.hbSendMs);
  }

  private stopHeartbeat(): void {
    if (this.hbTimer) {
      clearInterval(this.hbTimer);
      this.hbTimer = null;
    }
  }

  private dispatchData(data: unknown): void {
    const normalized = normalizeIncoming(data);
    if (normalized == null) return;
    if (typeof normalized === "string") {
      if (normalized === "\n" || normalized === "\r\n" || normalized === "\0") {
        return;
      }
      const frame = parseTextFrame(normalized);
      if (frame) this.opts.onMessage(frame);
      return;
    }
    const frame = parseBinaryFrame(normalized);
    if (frame) this.opts.onMessage(frame);
  }

  sendRaw(data: string | ArrayBuffer | Uint8Array): void {
    if (!this.ws || this.ws.readyState !== WS_OPEN) {
      throw new Error("STOMP socket not open");
    }
    this.ws.send(data);
  }

  sendJson(destination: string, payload: unknown): void {
    const body = JSON.stringify(payload);
    const frame =
      `SEND\n` +
      `destination:${destination}\n` +
      `content-type:application/json\n` +
      `content-length:${new TextEncoder().encode(body).length}\n` +
      `\n` +
      `${body}\0`;
    this.sendRaw(frame);
  }

  subscribe(id: string, destination: string): void {
    this.sendRaw(`SUBSCRIBE\nid:${id}\ndestination:${destination}\n\n\0`);
  }

  disconnect(): void {
    this.connected = false;
    this.stopHeartbeat();
    try {
      if (this.ws && this.ws.readyState === WS_OPEN) {
        this.sendRaw("DISCONNECT\n\n\0");
        this.ws.close();
      }
    } catch {
      /* ignore */
    }
    this.ws = null;
  }
}

/** Build browser SDK URL with query auth (headers not available in browsers). */
export function buildBrowserSdkUrl(
  serverUrl: string,
  tokens: {
    idToken: string;
    accessToken: string;
    profile: string;
    sdkVersion: string;
  }
): string {
  const u = new URL(serverUrl);
  u.searchParams.set("sdk-id-token", tokens.idToken);
  u.searchParams.set("sdk-access-token", tokens.accessToken);
  u.searchParams.set("kiponos-id", tokens.profile);
  u.searchParams.set("sdk-version", tokens.sdkVersion);
  return u.toString();
}

export function buildSdkAuthHeaders(tokens: {
  idToken: string;
  accessToken: string;
  profile: string;
  sdkVersion: string;
}): SdkAuthHeaders {
  return {
    "sdk-id-token": tokens.idToken,
    "sdk-access-token": tokens.accessToken,
    "kiponos-id": tokens.profile,
    "sdk-version": tokens.sdkVersion,
  };
}
