/**
 * Live Theme for Angular Admin Without an SPA Redeploy
 * Node server peer using @kiponos/angular (never browser Connect tokens).
 *
 *   npm install
 *   export KIPONOS_ID=… KIPONOS_ACCESS=…
 *   export KIPONOS="['MyApp']['1.0']['Dev']['base']"
 *   npm start
 *   node peer.mjs <value>
 */
import { Kiponos } from "@kiponos/angular/server";

const value = process.argv[2] || "night";

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("ui");
const path = kip.path("ui");
await path.set("theme", String(value));
await path.set("theme-set-at", new Date().toISOString());
await path.set("theme-set-by", "node-angular-peer");

console.log("wrote ui/theme =", path.get("theme"));
console.log("  set-at =", path.get("theme-set-at"));
console.log("teamId", kip.teamId);
kip.disconnect();
process.exit(0);
