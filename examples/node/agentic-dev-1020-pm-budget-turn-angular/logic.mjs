/**
 * Pure hot-path for agentic-dev-1020-pm-budget-turn (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "max-tokens";
export const DEFAULT = "8000";
export const FOLDER = "agentic-dev-1020-pm-budget-turn";
export const PATH = "examples/agentic-dev-1020-pm-budget-turn/max-tokens";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const cap = Number.parseInt(v, 10);
  const n = Number.isFinite(cap) ? cap : 8000;
  const proceed = n > 0;
  return { path: PATH, key: KEY, value: String(n), action: proceed ? "within_token_budget" : "stop_turn_budget", proceed, peers: PEERS };
}
