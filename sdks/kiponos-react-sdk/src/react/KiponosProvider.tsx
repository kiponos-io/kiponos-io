import {
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { Kiponos } from "../core/kiponos";
import { KiponosClient } from "../core/kiponos-client";
import type { FromEnvOptions } from "../core/env";
import type { KiponosStatus } from "../core/types";
import { KiponosReactContext } from "./context";

export interface KiponosProviderProps {
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
export function KiponosProvider({
  children,
  client: externalClient,
  fromEnv = false,
  autoConnect = true,
}: KiponosProviderProps) {
  const [ownedClient, setOwnedClient] = useState<KiponosClient | null>(null);
  const client = externalClient ?? ownedClient;

  const [status, setStatus] = useState<KiponosStatus>(
    client?.status ?? "idle"
  );
  const [error, setError] = useState<Error | null>(client?.error ?? null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    if (externalClient) return;

    if (!fromEnv) {
      setError(
        new Error(
          "[@kiponos/react] KiponosProvider needs client={...} from createFromEnv() " +
            "or fromEnv on a Node server runtime."
        )
      );
      return;
    }

    try {
      const opts: FromEnvOptions =
        typeof fromEnv === "object" ? fromEnv : {};
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

    if (
      autoConnect &&
      client.status !== "ready" &&
      client.status !== "connecting"
    ) {
      void client.connect().catch((e: Error) => {
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
      treeEpoch: tick,
    }),
    [client, status, error, tick]
  );

  return (
    <KiponosReactContext.Provider value={value}>
      {children}
    </KiponosReactContext.Provider>
  );
}
