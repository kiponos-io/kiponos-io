/**
 * Rate Limits the BFF Honors Without Redeploying Node
 * Node server peer using @kiponos/react (never browser Connect tokens).
 *
 *   npm install
 *   export KIPONOS_ID=… KIPONOS_ACCESS=…
 *   export KIPONOS="['MyApp']['1.0']['Dev']['base']"
 *   npm start
 *   node peer.mjs <value>
 */
import { Kiponos } from "@kiponos/react/server";

const value = process.argv[2] || "40";

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("limits");
const path = kip.path("limits");
await path.set("rps-cap", String(value));
await path.set("rps-cap-set-at", new Date().toISOString());
await path.set("rps-cap-set-by", "node-react-peer");

console.log("wrote limits/rps-cap =", path.get("rps-cap"));
console.log("  set-at =", path.get("rps-cap-set-at"));
console.log("teamId", kip.teamId);
kip.disconnect();
process.exit(0);
