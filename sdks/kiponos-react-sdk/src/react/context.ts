import { createContext, useContext } from "react";
import type { KiponosClient } from "../core/kiponos-client";
import type { KiponosStatus } from "../core/types";

export interface KiponosContextValue {
  client: KiponosClient | null;
  status: KiponosStatus;
  error: Error | null;
  ready: boolean;
  /** Bumps when config tree changes (Provider re-render signal). */
  treeEpoch?: number;
}

export const KiponosReactContext = createContext<KiponosContextValue | null>(
  null
);

export function useKiponosContext(): KiponosContextValue {
  const ctx = useContext(KiponosReactContext);
  if (!ctx) {
    throw new Error(
      "useKiponos* hooks require <KiponosProvider> in the component tree"
    );
  }
  return ctx;
}
