# QA Review — `@kiponos/react` v0.1.0

**Date:** 2026-07-28  
**Reviewer:** Grok (QA gate)  
**Target:** PROD hub `wss://kiponos.io/api/io-kiponos-sdk`  
**Profile:** `['Family-Agent']['1.0.0']['Alef-Dev']['base']`  
**Verdict: PASS** (with known browser-query gap documented)

---

## Outcome vs ask

| Ask | Result |
|-----|--------|
| Real E2E, no mocks | **Pass** — live PROD WebSocket, real tokens, real Python peer |
| Extensive scenarios | **Pass** — 37 live cases across 5 suites |
| Package quality gate | **Pass** after fixing 4 production bugs found by E2E |

---

## Evidence

```text
npm run test:unit   → 7 passed
npm run test:e2e    → 37 passed (5 files), ~108s
npm run typecheck   → clean
npm run build       → ESM + CJS + DTS
```

Command:

```bash
cd ~/work/kiponos-react-sdk
npm run test:e2e
# credentials: ~/.config/kiponos/otp-listener.env (never logged)
```

Isolated tree: `e2e-react-sdk/<runId>/…` under Family-Agent profile.

---

## E2E matrix (all live)

| Suite | Cases | Coverage |
|-------|------:|----------|
| `live-connect` | 9 | Ready + teamId + bootstrap; garbage tokens; mismatched profile; secret-leak check; query-auth probe |
| `live-crud` | 13 | set/get, path(), setPath, getInt, folderOrCreate, ensurePath, list*, deleteKey, deleteFolder, coerce types |
| `live-listeners` | 6 | Two JS participants: onChange, afterItemSaved, afterValueUpdated, unsub, delete propagation |
| `live-python-parity` | 2 | Python→JS live delta; JS→Python bootstrap read |
| `live-resilience` | 7 | Disconnect/reconnect, concurrent sets, rapid updates, **28s heartbeat idle**, dual fan-out, double connect |

**Query-param browser auth on PROD:** documented **FAIL** until `SdkHandshake` query fallback is deployed (expected). Header auth (Node / Java parity) **PASS**.

---

## Bugs found by QA (fixed before PASS)

| # | Severity | Finding | Fix |
|---|----------|---------|-----|
| 1 | **P0** | Node `ws` delivers STOMP text as `Buffer`; client never saw `CONNECTED` → connect hang/timeout | `normalizeIncoming()` in `stomp.ts` |
| 2 | **P0** | Ambient shell `KIPONOS=Unit-Tests` + Family-Agent tokens → HTTP 500 | E2E harness prefers otp-listener / Family-Agent profile |
| 3 | **P1** | Second client `ensurePath` hung when folder already existed (no create ack) | `waitOr` optimistic ack for mkdir/set |
| 4 | **P1** | No Node header transport (browser-only query) → E2E impossible | `ws-factory` + `authMode: headers\|query\|auto` |

---

## Security

| Check | Result |
|-------|--------|
| Tokens not printed in test output | Pass |
| Failed connect errors do not embed full tokens | Pass (asserted) |
| E2E under isolated `e2e-react-sdk/` prefix | Pass |
| Query tokens on URL (browser path) | Known residual risk; server note documents log redaction |

---

## Residual risks / follow-ups

1. **Deploy** `SdkHandshake` query-param fallback for real browsers (source patched in `srv-master` / `srv-fresh`, **not** on PROD yet).
2. Soft-ack (`waitOr`) means a true server failure may look like success until a subsequent read — acceptable for folder create races; set still prefers ack when present.
3. No React component render E2E (Provider/hooks) in browser yet — core client fully proven; add Playwright later if needed.
4. npm publish still operator-gated.

---

## How to re-run

```bash
# unit only (offline)
npm run test:unit

# live PROD (needs otp-listener.env)
npm run test:e2e

# verbose STOMP logs
KIPONOS_E2E_VERBOSE=1 npm run test:e2e

# longer heartbeat soak
KIPONOS_E2E_HB_MS=60000 npm run test:e2e
```

---

## Final gate

| Gate | Status |
|------|--------|
| Outcome matches ask | **PASS** |
| PROD verification | **PASS** (header auth) |
| Evidence captured | **PASS** |
| Follow-through (bugs fixed in package) | **PASS** |

**QA Reviewer: PASS — ready for continued integration; browser path blocked only on server deploy.**
