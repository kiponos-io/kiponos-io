/**
 * Pure hot-path for agentic-mcp-0915-am-execute-gate (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "execute-gate";
export const DEFAULT = "plan";
export const FOLDER = "agentic-mcp-0915-am-execute-gate";
export const PATH = "examples/agentic-mcp-0915-am-execute-gate/execute-gate";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const proceed = Boolean(v) && v.toLowerCase() !== "idle";
  return { path: PATH, key: KEY, value: v, action: proceed ? "execute_allowed" : "stay_in_plan", proceed, peers: PEERS };
}
