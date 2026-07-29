import { useEffect, useState } from "react";
import { parseDottedOrSlash } from "../core/paths";
import { useKiponosContext } from "./context";

export interface UseKiponosValueOptions {
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
export function useKiponosValue(
  keyOrPath: string,
  options: UseKiponosValueOptions = {}
): string | undefined {
  const { client, ready } = useKiponosContext();
  const { defaultValue, folders: foldersOpt } = options;

  const resolve = (): string | undefined => {
    if (!client) return defaultValue;
    if (foldersOpt) {
      return client.get(keyOrPath, defaultValue, ...foldersOpt);
    }
    // If path-like, treat as getPath
    if (keyOrPath.includes("/") || keyOrPath.includes(".")) {
      return client.getPath(keyOrPath, defaultValue);
    }
    return client.get(keyOrPath, defaultValue);
  };

  const [value, setValue] = useState<string | undefined>(resolve);

  useEffect(() => {
    setValue(resolve());
    if (!client) return;

    // Re-read on any tree change (simple + correct; fine for typical trees)
    const unsub = client.onTreeChanged(() => {
      setValue(resolve());
    });
    return unsub;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [client, ready, keyOrPath, defaultValue, foldersOpt?.join("/")]);

  return value;
}

/**
 * Live integer value (Java `getInt` style).
 */
export function useKiponosInt(
  keyOrPath: string,
  defaultValue = 0,
  folders?: string[]
): number {
  const raw = useKiponosValue(keyOrPath, {
    defaultValue: String(defaultValue),
    folders,
  });
  if (raw === undefined || raw === "") return defaultValue;
  const n = parseInt(raw, 10);
  return Number.isFinite(n) ? n : defaultValue;
}

/**
 * Split path helper for callers that want folders + key separately.
 */
export function splitPath(path: string): { folders: string[]; key: string } {
  const parts = parseDottedOrSlash(path);
  if (!parts.length) return { folders: [], key: "" };
  return { folders: parts.slice(0, -1), key: parts[parts.length - 1] };
}
