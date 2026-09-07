/**
 * Pure hot-path for agentic-mcp-0909-am-context-rot (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "compact-mode";
export const DEFAULT = "off";
export const FOLDER = "agentic-mcp-0909-am-context-rot";
export const PATH = "examples/agentic-mcp-0909-am-context-rot/compact-mode";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const live = ["yes", "live", "on", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: live ? "compact_on" : "full_dump", proceed: live, peers: PEERS };
}
