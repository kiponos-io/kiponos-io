/**
 * Cursor session posture on a live hub
 * react Node server peer — @kiponos/react/server
 * NEVER put Connect tokens in the browser / SPA.
 *
 *   npm install
 *   export KIPONOS_ID=… KIPONOS_ACCESS=… KIPONOS="['my-app']['v1.0.0']['dev']['base']"
 *   node peer.mjs [value]
 *   node peer.mjs --serve   # tiny BFF GET /posture  (tokens stay in this process)
 */
import { createServer } from "node:http";
import { Kiponos } from "@kiponos/react/server";
import { KEY, DEFAULT, FOLDER, decide } from "./logic.mjs";

const arg = process.argv[2];
const serve = arg === "--serve";

function apply(raw) {
  return decide(raw == null ? DEFAULT : raw);
}

if (!serve) {
  const d = apply(arg);
  console.log(d.path, "=>", d.value, "action=" + d.action, "proceed=" + d.proceed);
  if (process.env.KIPONOS_LIVE === "1") {
    const kip = Kiponos.createFromEnv({ quiet: true });
    await kip.connect();
    await kip.ensurePath("examples", FOLDER);
    const folder = kip.path("examples", FOLDER);
    if (!folder.get(KEY)) {
      await folder.set(KEY, DEFAULT);
    }
    const live = apply(folder.get(KEY) || DEFAULT);
    console.log("live", live.path, "=", live.value, "teamId", kip.teamId);
    kip.disconnect();
  }
  process.exit(0);
}

const kip = Kiponos.createFromEnv({ quiet: true });
await kip.connect();
await kip.ensurePath("examples", FOLDER);
const folder = kip.path("examples", FOLDER);
if (!folder.get(KEY)) {
  await folder.set(KEY, DEFAULT);
}
const port = Number(process.env.PORT || 0) || 0;
const server = createServer((req, res) => {
  if (req.url === "/health") {
    res.writeHead(200, { "content-type": "text/plain" });
    res.end("ok");
    return;
  }
  if (req.url === "/posture" || req.url === "/") {
    const d = apply(folder.get(KEY) || DEFAULT);
    res.writeHead(200, { "content-type": "application/json" });
    res.end(JSON.stringify({ ...d, sdk: "react-node-bff" }));
    return;
  }
  res.writeHead(404);
  res.end();
});
server.listen(port, "127.0.0.1", () => {
  const addr = server.address();
  console.log("react BFF listening", addr, "leaf examples/" + FOLDER + "/" + KEY);
  console.log("tokens stay in this process — SPA should call this BFF, not Connect");
});
