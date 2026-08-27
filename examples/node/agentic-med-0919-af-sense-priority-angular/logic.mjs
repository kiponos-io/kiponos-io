/**
 * Pure hot-path for agentic-med-0919-af-sense-priority (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "priority";
export const DEFAULT = "P3";
export const FOLDER = "agentic-med-0919-af-sense-priority";
export const PATH = "examples/agentic-med-0919-af-sense-priority/priority";
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
