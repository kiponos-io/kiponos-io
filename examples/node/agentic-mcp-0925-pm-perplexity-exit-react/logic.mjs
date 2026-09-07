/**
 * Pure hot-path for agentic-mcp-0925-pm-perplexity-exit (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "shared-truth";
export const DEFAULT = "live";
export const FOLDER = "agentic-mcp-0925-pm-perplexity-exit";
export const PATH = "examples/agentic-mcp-0925-pm-perplexity-exit/shared-truth";
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
