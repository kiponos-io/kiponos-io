/**
 * Pure hot-path for agentic-mcp-0913-pm-llm-skill-budget (angular Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "skill-budget";
export const DEFAULT = "on";
export const FOLDER = "agentic-mcp-0913-pm-llm-skill-budget";
export const PATH = "examples/agentic-mcp-0913-pm-llm-skill-budget/skill-budget";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const paused = ["on", "paused", "yes", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: paused ? "generated_skills_blocked" : "generated_skills_ok", proceed: !paused, peers: PEERS };
}
