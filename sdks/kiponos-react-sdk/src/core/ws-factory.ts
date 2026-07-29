/**
 * Create a WebSocket for browser or Node.
 * Node: optional HTTP headers (Java/Python parity) via the `ws` package.
 * Browser: native WebSocket (no custom headers — use query auth).
 */

export type SdkAuthHeaders = {
  "sdk-id-token": string;
  "sdk-access-token": string;
  "kiponos-id": string;
  "sdk-version": string;
};

/** Minimal WebSocket surface used by StompSocket. */
export interface SdkWebSocket {
  readonly readyState: number;
  binaryType: string;
  send(data: string | ArrayBuffer | Uint8Array): void;
  close(): void;
  onopen: ((ev?: unknown) => void) | null;
  onmessage: ((ev: { data: unknown }) => void) | null;
  onerror: ((ev?: unknown) => void) | null;
  onclose: ((ev?: unknown) => void) | null;
}

export const WS_OPEN = 1;

function isBrowser(): boolean {
  return typeof globalThis !== "undefined" && typeof (globalThis as { document?: unknown }).document !== "undefined";
}

/**
 * @param url Clean server URL (no secrets in URL when using headers).
 * @param headers When provided and running in Node, attach as HTTP upgrade headers.
 */
export async function createSdkWebSocket(
  url: string,
  headers?: SdkAuthHeaders
): Promise<SdkWebSocket> {
  // Prefer global WebSocket when no custom headers required (browser, or undici)
  if (!headers && typeof globalThis.WebSocket !== "undefined") {
    const ws = new globalThis.WebSocket(url) as unknown as SdkWebSocket;
    ws.binaryType = "arraybuffer";
    return ws;
  }

  // Node with headers (or no global WebSocket): use `ws` package
  try {
    const mod = (await import("ws")) as {
      default?: unknown;
      WebSocket?: unknown;
    };
    // ws CJS/ESM interop: default | WebSocket | module itself
    const Ctor = (mod.default ?? mod.WebSocket ?? mod) as new (
      u: string,
      o?: { headers?: Record<string, string> }
    ) => {
      readyState: number;
      send: (d: unknown) => void;
      close: () => void;
      on: (ev: string, cb: (...args: unknown[]) => void) => void;
    };

    if (typeof Ctor !== "function") {
      throw new Error(`ws package did not export a constructor: ${typeof Ctor}`);
    }

    const raw = new Ctor(url, headers ? { headers } : undefined);

    const adapter: SdkWebSocket = {
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
      onclose: null,
    };

    // Surface HTTP handshake failures (status 500 etc.) — native only has opaque "error"
    raw.on("unexpected-response", (...args: unknown[]) => {
      const res = args[1] as {
        statusCode?: number;
        headers?: Record<string, unknown>;
      };
      const status = res?.statusCode ?? 0;
      const err = new Error(
        `WebSocket handshake rejected HTTP ${status} (check tokens/profile match)`
      );
      adapter.onerror?.(err);
      try {
        raw.close();
      } catch {
        /* ignore */
      }
    });

    raw.on("open", () => adapter.onopen?.());
    raw.on("message", (...args: unknown[]) => {
      const data = args[0];
      let payload: unknown = data;
      if (data instanceof ArrayBuffer) {
        payload = data;
      } else if (ArrayBuffer.isView(data)) {
        const view = data as ArrayBufferView;
        payload = view.buffer.slice(
          view.byteOffset,
          view.byteOffset + view.byteLength
        );
      } else if (typeof data === "string") {
        payload = data;
      }
      adapter.onmessage?.({ data: payload });
    });
    raw.on("error", (...args: unknown[]) => {
      const e = args[0];
      if (e instanceof Error) adapter.onerror?.(e);
      else adapter.onerror?.(new Error(String(e ?? "WebSocket error")));
    });
    raw.on("close", () => adapter.onclose?.());

    return adapter;
  } catch (e) {
    if (typeof globalThis.WebSocket !== "undefined") {
      const ws = new globalThis.WebSocket(url) as unknown as SdkWebSocket;
      ws.binaryType = "arraybuffer";
      return ws;
    }
    throw new Error(
      `Cannot create WebSocket (need browser WebSocket or npm package "ws"). ${e}`
    );
  }
}

export { isBrowser };
