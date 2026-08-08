        /**
         * Kill the Path Everywhere — JVM, Agent, React BFF, Angular Admin
         * angular Node peer — same Team profile as Java/Python/other SDKs.
         * Never put Connect tokens in the browser. This process holds identity.
         *
         *   npm install && export KIPONOS_ID=… KIPONOS_ACCESS=… KIPONOS=…
         *   node peer.mjs [value]
         */
        import { Kiponos } from "@kiponos/angular/server";

        const value = process.argv[2] || "on";
        const kip = Kiponos.createFromEnv({ quiet: true });
        await kip.connect();
        await kip.ensurePath("incident");
const path = kip.path("incident");
        await path.set("path-enabled", String(value));
        await path.set("path-enabled-set-at", new Date().toISOString());
        await path.set("path-enabled-set-by", "node-angular-peer");
        await path.set("path-enabled-peer", "angular");

        console.log("same Team leaf incident/path-enabled =", path.get("path-enabled"));
        console.log("  set-by", path.get("path-enabled-set-by"), "teamId", kip.teamId);
        kip.disconnect();
        process.exit(0);
