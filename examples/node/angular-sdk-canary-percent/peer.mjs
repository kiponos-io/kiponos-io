/**
 * Canary Percent Shared With Angular Admin Mirrors
 * Node server peer using @kiponos/angular (never browser Connect tokens).
 *
 *   npm install
 *   export KIPONOS_ID=… KIPONOS_ACCESS=…
 *   export KIPONOS="['MyApp']['1.0']['Dev']['base']"
 *   npm start
 *   node peer.mjs <value>
 */
import { Kiponos } from "@kiponos/angular/server";

const value = process.argv[2] || "5";

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("release");
const path = kip.path("release");
await path.set("canary-percent", String(value));
await path.set("canary-percent-set-at", new Date().toISOString());
await path.set("canary-percent-set-by", "node-angular-peer");

console.log("wrote release/canary-percent =", path.get("canary-percent"));
console.log("  set-at =", path.get("canary-percent-set-at"));
console.log("teamId", kip.teamId);
kip.disconnect();
process.exit(0);
