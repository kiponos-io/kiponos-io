/**
 * Pure hot-path for agentic-mcp-0913-am-skill-trust (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "skill-trust";
export const DEFAULT = "core,reviewed";
export const FOLDER = "agentic-mcp-0913-am-skill-trust";
export const PATH = "examples/agentic-mcp-0913-am-skill-trust/skill-trust";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const proceed = Boolean(v);
  return { path: PATH, key: KEY, value: v, action: proceed ? "honor_trusted_skills" : "skill_not_trusted", proceed, peers: PEERS };
}
