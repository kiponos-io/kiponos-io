/**
 * Pure hot-path for agentic-mcp-0929-am-stdio-lifetime (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "mcp-drain";
export const DEFAULT = "off";
export const FOLDER = "agentic-mcp-0929-am-stdio-lifetime";
export const PATH = "examples/agentic-mcp-0929-am-stdio-lifetime/mcp-drain";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const paused = ["on", "paused", "yes", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: paused ? "drain_mcp_children" : "children_live", proceed: !paused, peers: PEERS };
}
