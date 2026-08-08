        /**
         * Four SDKs, One Team Tree — Web, Server, and Users Move Together
         * angular Node peer — same Team profile as Java/Python/other SDKs.
         * Never put Connect tokens in the browser. This process holds identity.
         *
         *   npm install && export KIPONOS_ID=… KIPONOS_ACCESS=… KIPONOS=…
         *   node peer.mjs [value]
         */
        import { Kiponos } from "@kiponos/angular/server";

        const value = process.argv[2] || "steady";
        const kip = Kiponos.createFromEnv({ quiet: true });
        await kip.connect();
        await kip.ensurePath("examples", "quad-sdk-live-mesh");
const path = kip.path("examples", "quad-sdk-live-mesh");
        await path.set("mode", String(value));
        await path.set("mode-set-at", new Date().toISOString());
        await path.set("mode-set-by", "node-angular-peer");
        await path.set("mode-peer", "angular");

        console.log("same Team leaf examples/quad-sdk-live-mesh/mode =", path.get("mode"));
        console.log("  set-by", path.get("mode-set-by"), "teamId", kip.teamId);
        kip.disconnect();
        process.exit(0);
