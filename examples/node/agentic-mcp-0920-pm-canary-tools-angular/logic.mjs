/**
 * Pure hot-path for agentic-mcp-0920-pm-canary-tools (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "canary-percent";
export const DEFAULT = "0";
export const FOLDER = "agentic-mcp-0920-pm-canary-tools";
export const PATH = "examples/agentic-mcp-0920-pm-canary-tools/canary-percent";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const cap = Number.parseInt(v, 10);
  const n = Number.isFinite(cap) ? cap : 0;
  const proceed = n > 0;
  return { path: PATH, key: KEY, value: String(n), action: proceed ? "canary_tools_live" : "canary_tools_off", proceed, peers: PEERS };
}
