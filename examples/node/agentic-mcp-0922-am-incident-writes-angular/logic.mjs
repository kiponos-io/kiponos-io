/**
 * Pure hot-path for agentic-mcp-0922-am-incident-writes (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "incident-pause";
export const DEFAULT = "off";
export const FOLDER = "agentic-mcp-0922-am-incident-writes";
export const PATH = "examples/agentic-mcp-0922-am-incident-writes/incident-pause";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const paused = ["on", "paused", "yes", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: paused ? "freeze_shopping_writes" : "shopping_path_live", proceed: !paused, peers: PEERS };
}
