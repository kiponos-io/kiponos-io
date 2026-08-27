/**
 * Pure hot-path for agentic-med-0930-af-agent-folder (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "owner-agent";
export const DEFAULT = "travel-coordinator";
export const FOLDER = "agentic-med-0930-af-agent-folder";
export const PATH = "examples/agentic-med-0930-af-agent-folder/owner-agent";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const proceed = Boolean(v);
  return { path: PATH, key: KEY, value: v, action: proceed ? "honor_chosen_owner" : "refuse_unowned_write", proceed, peers: PEERS };
}
