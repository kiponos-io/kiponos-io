/**
 * Manual connect probe — no secret printing.
 */
import { readFileSync, existsSync } from "node:fs";
import { homedir } from "node:os";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
// Use built package after build, else ts via vitest path is hard — use src via dynamic
// Prefer dist
const root = join(dirname(fileURLToPath(import.meta.url)), "..");

function loadEnv(path) {
  const out = {};
  if (!existsSync(path)) return out;
  for (const line of readFileSync(path, "utf8").split("\n")) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const eq = t.indexOf("=");
    if (eq < 1) continue;
    const k = t.slice(0, eq).trim();
    let v = t.slice(eq + 1).trim();
    if (
      (v.startsWith('"') && v.endsWith('"')) ||
      (v.startsWith("'") && v.endsWith("'"))
    )
      v = v.slice(1, -1);
    out[k] = v;
  }
  return out;
}

const fileEnv = loadEnv(join(homedir(), ".config/kiponos/otp-listener.env"));
const profile =
  fileEnv.KIPONOS && /\['/.test(fileEnv.KIPONOS)
    ? fileEnv.KIPONOS
    : "['Family-Agent']['1.0.0']['Alef-Dev']['base']";

console.log("profile", profile);
console.log("id_len", fileEnv.KIPONOS_ID?.length);

// Import from dist
const { KiponosClient } = await import(join(root, "dist/index.js"));

const client = new KiponosClient({
  profile,
  idToken: fileEnv.KIPONOS_ID,
  accessToken: fileEnv.KIPONOS_ACCESS,
  authMode: "headers",
  quiet: false,
  autoConnect: false,
});

try {
  await client.connect();
  console.log("READY", client.ready, "team", client.teamId);
  console.log("folders", client.listFolders());
  await client.ensurePath("e2e-react-sdk", "debug");
  await client.set("ping", String(Date.now()), "e2e-react-sdk", "debug");
  console.log("get", client.get("ping", null, "e2e-react-sdk", "debug"));
  client.disconnect();
  process.exit(0);
} catch (e) {
  console.error("FAIL", e?.message || e);
  process.exit(1);
}
