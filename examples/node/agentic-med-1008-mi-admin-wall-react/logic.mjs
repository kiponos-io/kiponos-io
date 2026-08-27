/**
 * Pure hot-path for agentic-med-1008-mi-admin-wall (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "wall-focus";
export const DEFAULT = "checkout";
export const FOLDER = "agentic-med-1008-mi-admin-wall";
export const PATH = "examples/agentic-med-1008-mi-admin-wall/wall-focus";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const proceed = Boolean(v) && v.toLowerCase() !== "idle";
  return { path: PATH, key: KEY, value: v, action: proceed ? "admin_wall_focus" : "admin_wall_idle", proceed, peers: PEERS };
}
