/**
 * Pure hot-path for agentic-mcp-0917-pm-code-mode (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "code-mode";
export const DEFAULT = "off";
export const FOLDER = "agentic-mcp-0917-pm-code-mode";
export const PATH = "examples/agentic-mcp-0917-pm-code-mode/code-mode";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const live = ["yes", "live", "on", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: live ? "code_mode_on" : "schema_dump_mode", proceed: live, peers: PEERS };
}
