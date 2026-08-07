/**
 * Feature Kill From the Hub — Angular Peer, Same Leaf as Java
 * Node server peer using @kiponos/angular (never browser Connect tokens).
 *
 *   npm install
 *   export KIPONOS_ID=… KIPONOS_ACCESS=…
 *   export KIPONOS="['MyApp']['1.0']['Dev']['base']"
 *   npm start
 *   node peer.mjs <value>
 */
import { Kiponos } from "@kiponos/angular/server";

const value = process.argv[2] || "off";

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("flags");
const path = kip.path("flags");
await path.set("feature-x", String(value));
await path.set("feature-x-set-at", new Date().toISOString());
await path.set("feature-x-set-by", "node-angular-peer");

console.log("wrote flags/feature-x =", path.get("feature-x"));
console.log("  set-at =", path.get("feature-x-set-at"));
console.log("teamId", kip.teamId);
kip.disconnect();
process.exit(0);
