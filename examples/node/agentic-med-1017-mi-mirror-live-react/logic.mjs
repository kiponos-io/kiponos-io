/**
 * Pure hot-path for agentic-med-1017-mi-mirror-live (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "device-live";
export const DEFAULT = "yes";
export const FOLDER = "agentic-med-1017-mi-mirror-live";
export const PATH = "examples/agentic-med-1017-mi-mirror-live/device-live";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const live = ["yes", "live", "on", "true"].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: live ? "mirror_device_live" : "route_other_mirror_device", proceed: live, peers: PEERS };
}
