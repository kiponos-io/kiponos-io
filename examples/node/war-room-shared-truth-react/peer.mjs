        /**
         * One War-Room Headline: Humans, Agents, and Every SDK on the Same Leaf
         * react Node peer — same Team profile as Java/Python/other SDKs.
         * Never put Connect tokens in the browser. This process holds identity.
         *
         *   npm install && export KIPONOS_ID=… KIPONOS_ACCESS=… KIPONOS=…
         *   node peer.mjs [value]
         */
        import { Kiponos } from "@kiponos/react/server";

        const value = process.argv[2] || "steady";
        const kip = Kiponos.createFromEnv({ quiet: true });
        await kip.connect();
        await kip.ensurePath("warroom");
const path = kip.path("warroom");
        await path.set("headline", String(value));
        await path.set("headline-set-at", new Date().toISOString());
        await path.set("headline-set-by", "node-react-peer");
        await path.set("headline-peer", "react");

        console.log("same Team leaf warroom/headline =", path.get("headline"));
        console.log("  set-by", path.get("headline-set-by"), "teamId", kip.teamId);
        kip.disconnect();
        process.exit(0);
