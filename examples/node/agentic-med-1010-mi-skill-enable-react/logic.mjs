/**
 * Pure hot-path for agentic-med-1010-mi-skill-enable (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "enabled-set";
export const DEFAULT = "research,notify";
export const FOLDER = "agentic-med-1010-mi-skill-enable";
export const PATH = "examples/agentic-med-1010-mi-skill-enable/enabled-set";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const proceed = Boolean(v);
  return { path: PATH, key: KEY, value: v, action: proceed ? "honor_enabled_skills" : "skill_not_enabled", proceed, peers: PEERS };
}
