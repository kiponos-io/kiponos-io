/**
 * Incident Pause That Freezes Risky UI Paths Instantly
 * Node server peer using @kiponos/react (never browser Connect tokens).
 *
 *   npm install
 *   export KIPONOS_ID=… KIPONOS_ACCESS=…
 *   export KIPONOS="['MyApp']['1.0']['Dev']['base']"
 *   npm start
 *   node peer.mjs <value>
 */
import { Kiponos } from "@kiponos/react/server";

const value = process.argv[2] || "off";

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("incident");
const path = kip.path("incident");
await path.set("pause-risky", String(value));
await path.set("pause-risky-set-at", new Date().toISOString());
await path.set("pause-risky-set-by", "node-react-peer");

console.log("wrote incident/pause-risky =", path.get("pause-risky"));
console.log("  set-at =", path.get("pause-risky-set-at"));
console.log("teamId", kip.teamId);
kip.disconnect();
process.exit(0);
