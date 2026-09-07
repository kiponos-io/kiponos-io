/**
 * Pure hot-path for agentic-mcp-0922-pm-sense-stale (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "priority";
export const DEFAULT = "P3";
export const FOLDER = "agentic-mcp-0922-pm-sense-stale";
export const PATH = "examples/agentic-mcp-0922-pm-sense-stale/priority";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const u = v.toUpperCase();
  const abort = u.startsWith("P0") || u.startsWith("P1");
  return { path: PATH, key: KEY, value: v, action: abort ? "abort_mid_turn_no_restart" : "continue_turn", proceed: !abort, peers: PEERS };
}
