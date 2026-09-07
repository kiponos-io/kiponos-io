/**
 * Pure hot-path for agentic-mcp-0917-am-doom-loop (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "retry-max";
export const DEFAULT = "2";
export const FOLDER = "agentic-mcp-0917-am-doom-loop";
export const PATH = "examples/agentic-mcp-0917-am-doom-loop/retry-max";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const cap = Number.parseInt(v, 10);
  const n = Number.isFinite(cap) ? cap : 2;
  const proceed = n > 0;
  return { path: PATH, key: KEY, value: String(n), action: proceed ? "retry_within_cap" : "doom_loop_stopped", proceed, peers: PEERS };
}
