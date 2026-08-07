/**
 * A/B Weights You Can Rebalance Mid-Experiment
 * Node server peer using @kiponos/react (never browser Connect tokens).
 *
 *   npm install
 *   export KIPONOS_ID=… KIPONOS_ACCESS=…
 *   export KIPONOS="['MyApp']['1.0']['Dev']['base']"
 *   npm start
 *   node peer.mjs <value>
 */
import { Kiponos } from "@kiponos/react/server";

const value = process.argv[2] || "50,50";

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("experiments");
const path = kip.path("experiments");
await path.set("ab-weights", String(value));
await path.set("ab-weights-set-at", new Date().toISOString());
await path.set("ab-weights-set-by", "node-react-peer");

console.log("wrote experiments/ab-weights =", path.get("ab-weights"));
console.log("  set-at =", path.get("ab-weights-set-at"));
console.log("teamId", kip.teamId);
kip.disconnect();
process.exit(0);
