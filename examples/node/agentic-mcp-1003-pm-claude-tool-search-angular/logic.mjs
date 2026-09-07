/**
 * Pure hot-path for agentic-mcp-1003-pm-claude-tool-search (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "tool-search";
export const DEFAULT = "on";
export const FOLDER = "agentic-mcp-1003-pm-claude-tool-search";
export const PATH = "examples/agentic-mcp-1003-pm-claude-tool-search/tool-search";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const live = ["yes", "live", "on", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: live ? "defer_schemas" : "dump_all_schemas", proceed: live, peers: PEERS };
}
