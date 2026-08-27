/**
 * Pure hot-path for agentic-dev-0921-am-shopping-pause (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "incident-pause";
export const DEFAULT = "off";
export const FOLDER = "agentic-dev-0921-am-shopping-pause";
export const PATH = "examples/agentic-dev-0921-am-shopping-pause/incident-pause";
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
