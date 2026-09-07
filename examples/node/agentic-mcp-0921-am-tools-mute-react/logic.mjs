/**
 * Pure hot-path for agentic-mcp-0921-am-tools-mute (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "tools-mute";
export const DEFAULT = "none";
export const FOLDER = "agentic-mcp-0921-am-tools-mute";
export const PATH = "examples/agentic-mcp-0921-am-tools-mute/tools-mute";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const muted = !["none", "off", ""].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: muted ? "tool_logs_muted" : "tool_logs_live", proceed: !muted, peers: PEERS };
}
