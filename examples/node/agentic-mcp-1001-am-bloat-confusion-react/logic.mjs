/**
 * Pure hot-path for agentic-mcp-1001-am-bloat-confusion (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "schema-budget";
export const DEFAULT = "4000";
export const FOLDER = "agentic-mcp-1001-am-bloat-confusion";
export const PATH = "examples/agentic-mcp-1001-am-bloat-confusion/schema-budget";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const cap = Number.parseInt(v, 10);
  const n = Number.isFinite(cap) ? cap : 4000;
  const proceed = n > 0;
  return { path: PATH, key: KEY, value: String(n), action: proceed ? "schema_within_budget" : "schema_dump_blocked", proceed, peers: PEERS };
}
