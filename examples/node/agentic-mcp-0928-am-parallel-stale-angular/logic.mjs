/**
 * Pure hot-path for agentic-mcp-0928-am-parallel-stale (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "inherit-posture";
export const DEFAULT = "on";
export const FOLDER = "agentic-mcp-0928-am-parallel-stale";
export const PATH = "examples/agentic-mcp-0928-am-parallel-stale/inherit-posture";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const live = ["yes", "live", "on", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: live ? "child_reads_hub" : "child_frozen_argv", proceed: live, peers: PEERS };
}
