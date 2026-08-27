/**
 * Pure hot-path for agentic-med-0921-mi-mcp-gate (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "tools-allow";
export const DEFAULT = "search,read";
export const FOLDER = "agentic-med-0921-mi-mcp-gate";
export const PATH = "examples/agentic-med-0921-mi-mcp-gate/tools-allow";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const parts = new Set(v.split(",").map((p) => p.trim().toLowerCase()).filter(Boolean));
  const proceed = !parts.has("write");
  return { path: PATH, key: KEY, value: v, action: proceed ? "allow_listed_tools" : "deny_write_no_mcp_restart", proceed, peers: PEERS };
}
