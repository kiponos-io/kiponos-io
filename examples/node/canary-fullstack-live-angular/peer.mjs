        /**
         * Canary Percent That Moves BFF, Workers, and UI Mirrors Together
         * angular Node peer — same Team profile as Java/Python/other SDKs.
         * Never put Connect tokens in the browser. This process holds identity.
         *
         *   npm install && export KIPONOS_ID=… KIPONOS_ACCESS=… KIPONOS=…
         *   node peer.mjs [value]
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
        await path.set("canary-percent-peer", "angular");

        console.log("same Team leaf release/canary-percent =", path.get("canary-percent"));
        console.log("  set-by", path.get("canary-percent-set-by"), "teamId", kip.teamId);
        kip.disconnect();
        process.exit(0);
