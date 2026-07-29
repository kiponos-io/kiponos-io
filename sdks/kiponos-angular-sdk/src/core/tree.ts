import type { ConfigNode, ConfigTree } from "./types";

export const VALUE_KEY = "value";

export function isKeyNode(node: unknown): node is { value: string } {
  return (
    typeof node === "object" &&
    node !== null &&
    VALUE_KEY in node &&
    Object.keys(node as object).length === 1
  );
}

export function resolveNode(tree: ConfigTree, folders: string[]): ConfigTree {
  let node: ConfigNode | ConfigTree = tree;
  for (const f of folders) {
    if (typeof node !== "object" || node === null || !(f in node)) {
      throw new Error(`Folder not found: ${folders.join("/")} (at ${JSON.stringify(f)})`);
    }
    const next = (node as ConfigTree)[f];
    if (isKeyNode(next)) {
      throw new Error(`Path hits a key, not a folder: ${f}`);
    }
    node = next as ConfigTree;
  }
  if (typeof node !== "object" || node === null) {
    throw new Error(`Not a folder: ${folders.join("/")}`);
  }
  return node as ConfigTree;
}

/** Ensure intermediate folders exist in the local tree (does not hit server). */
export function ensureLocalFolders(tree: ConfigTree, folders: string[]): ConfigTree {
  let parent = tree;
  for (const f of folders) {
    const cur = parent[f];
    if (cur === undefined || isKeyNode(cur)) {
      parent[f] = {};
    }
    parent = parent[f] as ConfigTree;
  }
  return parent;
}

export function applyValueAt(
  tree: ConfigTree,
  folders: string[],
  key: string,
  value: string
): void {
  const parent =
    folders.length === 0 ? tree : ensureLocalFolders(tree, folders);
  parent[key] = { [VALUE_KEY]: value };
}

export function getValueAt(
  tree: ConfigTree,
  folders: string[],
  key: string,
  defaultValue?: string
): string | undefined {
  try {
    const parent = folders.length ? resolveNode(tree, folders) : tree;
    const node = parent[key];
    if (isKeyNode(node)) return node.value;
    return defaultValue;
  } catch {
    return defaultValue;
  }
}

export function listKeysAt(tree: ConfigTree, folders: string[]): string[] {
  const parent = folders.length ? resolveNode(tree, folders) : tree;
  return Object.keys(parent)
    .filter((k) => isKeyNode(parent[k]))
    .sort();
}

export function listFoldersAt(tree: ConfigTree, folders: string[]): string[] {
  const parent = folders.length ? resolveNode(tree, folders) : tree;
  return Object.keys(parent)
    .filter((k) => {
      const n = parent[k];
      return typeof n === "object" && n !== null && !isKeyNode(n);
    })
    .sort();
}

export function deepCloneTree(tree: ConfigTree): ConfigTree {
  return JSON.parse(JSON.stringify(tree)) as ConfigTree;
}
