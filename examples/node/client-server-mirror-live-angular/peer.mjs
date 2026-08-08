        /**
         * Client Mirrors Server Truth — Without Putting Tokens in the Browser
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
        await kip.ensurePath("mirror");
const path = kip.path("mirror");
        await path.set("truth", String(value));
        await path.set("truth-set-at", new Date().toISOString());
        await path.set("truth-set-by", "node-angular-peer");
        await path.set("truth-peer", "angular");

        console.log("same Team leaf mirror/truth =", path.get("truth"));
        console.log("  set-by", path.get("truth-set-by"), "teamId", kip.teamId);
        kip.disconnect();
        process.exit(0);
