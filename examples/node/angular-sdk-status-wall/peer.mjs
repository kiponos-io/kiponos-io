/**
 * Angular Status Wall Fed by the Same Hub as Everyone Else
 * Node server peer using @kiponos/angular (never browser Connect tokens).
 *
 *   npm install
 *   export KIPONOS_ID=… KIPONOS_ACCESS=…
 *   export KIPONOS="['MyApp']['1.0']['Dev']['base']"
 *   npm start
 *   node peer.mjs <value>
 */
import { Kiponos } from "@kiponos/angular/server";

const value = process.argv[2] || "steady";

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("ops");
const path = kip.path("ops");
await path.set("status-headline", String(value));
await path.set("status-headline-set-at", new Date().toISOString());
await path.set("status-headline-set-by", "node-angular-peer");

console.log("wrote ops/status-headline =", path.get("status-headline"));
console.log("  set-at =", path.get("status-headline-set-at"));
console.log("teamId", kip.teamId);
kip.disconnect();
process.exit(0);
