/**
 * Pure hot-path for agentic-frameworks-missing-hub (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "shared-truth";
export const DEFAULT = "live";
export const FOLDER = "agentic-frameworks-missing-hub";
export const PATH = "examples/agentic-frameworks-missing-hub/shared-truth";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const live = ["live", "yes", "on"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: live ? "peers_share_live_hub" : "refuse_stale_host_argv", proceed: live, peers: PEERS };
}
