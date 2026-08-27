/**
 * Pure hot-path for agentic-dev-0831-am-chat-mute (react Node BFF).
 * No tokens here — tests run offline.
 */
export const KEY = "chat-mute";
export const DEFAULT = "none";
export const FOLDER = "agentic-dev-0831-am-chat-mute";
export const PATH = "examples/agentic-dev-0831-am-chat-mute/chat-mute";
export const PEERS = ["java", "python", "react-node", "angular-node"];

function norm(value) {
  const v = (value == null ? DEFAULT : String(value)).trim();
  return v || DEFAULT;
}

export function decide(value) {
  const v = norm(value);
  const muted = !["none", "off", ""].includes(v.toLowerCase());
  return { path: PATH, key: KEY, value: v, action: muted ? "mute_sends_keep_session" : "group_chat_sends_live", proceed: !muted, peers: PEERS };
}
