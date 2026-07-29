/**
 * Kiponos React/Node hub peer — install from npm:
 *   npm install @kiponos/react
 *
 * Env (same as Java SDK):
 *   KIPONOS_ID, KIPONOS_ACCESS, KIPONOS (profile)
 *
 * Usage:
 *   npm start
 *   node status-wall.mjs focus
 *   node status-wall.mjs online "shipping feature"
 */
import { Kiponos } from "@kiponos/react/server";

const status = process.argv[2] || "online";
const note = process.argv[3] || "npm example peer";

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("demo", "status-wall");
const wall = kip.path("demo", "status-wall");
await wall.set("status-alpha", status);
await wall.set("last-ping", new Date().toISOString());
await wall.set("note", note);

console.log("wrote demo/status-wall/*");
console.log("  status-alpha =", wall.get("status-alpha"));
console.log("  last-ping    =", wall.get("last-ping"));
console.log("  note         =", wall.get("note"));
console.log("teamId", kip.teamId);
kip.disconnect();
process.exit(0);
