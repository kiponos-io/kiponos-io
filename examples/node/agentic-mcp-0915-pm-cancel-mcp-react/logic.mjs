/**
 * Pure hot-path for agentic-mcp-0915-pm-cancel-mcp (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "cancel-token";
export const DEFAULT = "off";
export const FOLDER = "agentic-mcp-0915-pm-cancel-mcp";
export const PATH = "examples/agentic-mcp-0915-pm-cancel-mcp/cancel-token";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const paused = ["on", "paused", "yes", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: paused ? "cancel_mcp_task" : "mcp_task_live", proceed: !paused, peers: PEERS };
}
