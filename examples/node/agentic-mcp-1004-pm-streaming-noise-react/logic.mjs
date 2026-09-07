/**
 * Pure hot-path for agentic-mcp-1004-pm-streaming-noise (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "result-cap";
export const DEFAULT = "2000";
export const FOLDER = "agentic-mcp-1004-pm-streaming-noise";
export const PATH = "examples/agentic-mcp-1004-pm-streaming-noise/result-cap";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const cap = Number.parseInt(v, 10);
  const n = Number.isFinite(cap) ? cap : 2000;
  const proceed = n > 0;
  return { path: PATH, key: KEY, value: String(n), action: proceed ? "result_projected" : "result_too_fat", proceed, peers: PEERS };
}
