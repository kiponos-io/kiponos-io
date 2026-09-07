/**
 * Pure hot-path for agentic-mcp-0918-pm-oauth-not-knob (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "session-posture";
export const DEFAULT = "focus=admin-wall,shopping-pause=off";
export const FOLDER = "agentic-mcp-0918-pm-oauth-not-knob";
export const PATH = "examples/agentic-mcp-0918-pm-oauth-not-knob/session-posture";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const paused = v.toLowerCase().includes("shopping-pause=on") || v.toLowerCase().includes("pause=on");
  return { path: PATH, key: KEY, value: v, action: paused ? "incident_pause_active" : "share_session_posture", proceed: !paused, peers: PEERS };
}
