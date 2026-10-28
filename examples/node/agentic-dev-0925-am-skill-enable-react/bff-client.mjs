/**
 * Tiny BFF client for agentic-dev-0925-am-skill-enable (react).
 * SPA calls this process — never Connect tokens in the browser.
 *
 *   node peer.mjs --serve
 *   node bff-client.mjs http://127.0.0.1:PORT
 */
import { PATH, decide } from "./logic.mjs";

const base = process.argv[2] || "http://127.0.0.1:8780";
const res = await fetch(base.replace(/\/$/, "") + "/posture");
if (!res.ok) {
  console.error("BFF", res.status, await res.text());
  process.exit(1);
}
const body = await res.json();
console.log("leaf", PATH);
console.log("posture", body);
console.log("local decide check", decide(body.value).action);
