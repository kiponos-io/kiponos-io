/**
 * E2E: JS client ↔ Python agent_client cross-participant on same PROD hub.
 * Proves React/Node package is a true peer of the Python SDK.
 */
import { describe, it, expect, beforeAll, afterAll } from "vitest";
import { spawnSync } from "node:child_process";
import { writeFileSync, unlinkSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import {
  createLiveClient,
  credsPresent,
  uniqueRunId,
  E2E_ROOT,
  waitFor,
  sleep,
  FAMILY_PROFILE,
} from "./harness";
import type { KiponosClient } from "../../src/core/kiponos-client";

const hasCreds = credsPresent();
const runId = uniqueRunId();
const F = [E2E_ROOT, runId, "py-parity"] as const;
const AGENT_CLIENT = `${process.env.HOME}/.grok/skills/kiponos-sdk/scripts/agent_client.py`;
const HAS_PY = existsSync(AGENT_CLIENT);

function runPythonSet(key: string, value: string, folders: string[]): void {
  const script = `
import os, sys
sys.path.insert(0, ${JSON.stringify(join(process.env.HOME || "", ".grok/skills/kiponos-sdk/scripts"))})
from agent_client import KiponosAgentClient
folders = ${JSON.stringify(folders)}
key = ${JSON.stringify(key)}
value = ${JSON.stringify(value)}
profile = ${JSON.stringify(FAMILY_PROFILE)}
c = KiponosAgentClient(profile=profile, quiet=True)
c.connect()
c.ensure_path(*folders)
c.set(key, value, *folders)
got = c.get(key, None, *folders)
c.close()
assert got == value, (got, value)
print("OK")
`;
  const path = join(tmpdir(), `kiponos-e2e-py-${Date.now()}.py`);
  writeFileSync(path, script, "utf8");
  try {
    const envPath = join(
      process.env.HOME || "",
      ".config/kiponos/otp-listener.env"
    );
    // Load env into child without printing
    const r = spawnSync(
      "bash",
      [
        "-lc",
        `set -a; source "${envPath}"; set +a; python3 "${path}"`,
      ],
      { encoding: "utf8", timeout: 90000 }
    );
    if (r.status !== 0) {
      throw new Error(
        `Python set failed status=${r.status} stderr=${(r.stderr || "").slice(0, 400)} stdout=${(r.stdout || "").slice(0, 200)}`
      );
    }
    expect(r.stdout || "").toContain("OK");
  } finally {
    try {
      unlinkSync(path);
    } catch {
      /* ignore */
    }
  }
}

describe.skipIf(!hasCreds || !HAS_PY)(
  "E2E Python ↔ JS cross-participant (PROD)",
  () => {
    let js: KiponosClient;

    beforeAll(async () => {
      js = createLiveClient();
      await js.connect();
      await js.ensurePath(...F);
    }, 90000);

    afterAll(async () => {
      try {
        if (js?.ready) {
          for (const k of js.listKeys(...F)) {
            await js.deleteKey(k, ...F).catch(() => undefined);
          }
        }
      } catch {
        /* ignore */
      }
      js?.disconnect();
    }, 60000);

    it("Python set is observed by JS client local tree via live delta", async () => {
      const key = "from-python";
      const value = `py-${Date.now()}`;
      const pending = waitFor(
        () => js.get(key, undefined, ...F) === value,
        { timeoutMs: 45000, label: "js sees python write" }
      );
      // fire python shortly after waiter is armed
      await sleep(200);
      runPythonSet(key, value, [...F]);
      await pending;
      expect(js.get(key, undefined, ...F)).toBe(value);
    }, 120000);

    it("JS set is readable by a fresh Python process (bootstrap)", async () => {
      const key = "from-js";
      const value = `js-${Date.now()}`;
      await js.set(key, value, ...F);

      const script = `
import os, sys
sys.path.insert(0, ${JSON.stringify(join(process.env.HOME || "", ".grok/skills/kiponos-sdk/scripts"))})
from agent_client import KiponosAgentClient
folders = ${JSON.stringify([...F])}
key = ${JSON.stringify(key)}
expect = ${JSON.stringify(value)}
c = KiponosAgentClient(profile=${JSON.stringify(FAMILY_PROFILE)}, quiet=True)
c.connect()
got = c.get(key, None, *folders)
c.close()
assert got == expect, (got, expect)
print("OK")
`;
      const path = join(tmpdir(), `kiponos-e2e-py-read-${Date.now()}.py`);
      writeFileSync(path, script, "utf8");
      const envPath = join(
        process.env.HOME || "",
        ".config/kiponos/otp-listener.env"
      );
      try {
        const r = spawnSync(
          "bash",
          ["-lc", `set -a; source "${envPath}"; set +a; python3 "${path}"`],
          { encoding: "utf8", timeout: 90000 }
        );
        if (r.status !== 0) {
          throw new Error(
            `Python get failed: ${(r.stderr || r.stdout || "").slice(0, 500)}`
          );
        }
        expect(r.stdout || "").toContain("OK");
      } finally {
        try {
          unlinkSync(path);
        } catch {
          /* ignore */
        }
      }
    }, 120000);
  }
);
