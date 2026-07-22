package io.kiponos.examples.offline;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Offline / last-known-good read posture demo.
 *
 * Classic pain: config server blip becomes an app outage because every
 * feature flag and timeout is a remote call with no local memory.
 *
 * Kiponos fix: SDK keeps an in-process cache. This example models the
 * operational question — Live vs LKG vs Safe — and a timeout that still
 * works from the last known good value when the hub is unreachable.
 *
 * Note: full offline mode is an SDK runtime property; here we demonstrate
 * the *policy* layer you put around it (what to serve when not Live).
 */
public final class OfflineLkgApp {

    static final String EXAMPLES = "examples";
    static final String FOLDER = "api-10-offline-lkg-reads";

    static final String PAYMENT_TIMEOUT_MS = "payment-timeout-ms";
    static final int DEFAULT_TIMEOUT_MS = 2_500;

    public static void main(String[] args) throws InterruptedException {
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder folder = ensureFolder(kiponos);
            int timeout = readTimeoutMs(folder);
            SdkMode mode = detectMode(kiponos);

            ReadPosture posture = resolvePosture(mode, timeout, DEFAULT_TIMEOUT_MS);

            System.out.println("========================================");
            System.out.println("  Kiponos example: Offline / LKG reads");
            System.out.println("  folder: examples / api-10-offline-lkg-reads");
            System.out.println("  sdk-mode:            " + posture.mode());
            System.out.println("  payment-timeout-ms:  " + posture.effectiveTimeoutMs());
            System.out.println("  source:              " + posture.source());
            System.out.println("========================================");
            System.out.println(posture.summaryLine());
            System.out.println();
            System.out.println("Ops play: change payment-timeout-ms in the hub,");
            System.out.println("then re-run. When offline, last known good should hold.");

            Thread.sleep(3_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureFolder(Kiponos kiponos) {
        Folder root = kiponos.getRootFolder();
        if (root == null) {
            throw new IllegalStateException(
                    "Kiponos root is null — SDK not Ready. Check KIPONOS_ID / KIPONOS_ACCESS.");
        }
        Folder examples = root.folderOrCreate(EXAMPLES);
        Folder f = examples.folderOrCreate(FOLDER);
        if (!f.hasKey(PAYMENT_TIMEOUT_MS)) {
            f.set(PAYMENT_TIMEOUT_MS, String.valueOf(DEFAULT_TIMEOUT_MS));
            System.out.println("Created default payment-timeout-ms=" + DEFAULT_TIMEOUT_MS);
        }
        return f;
    }

    static int readTimeoutMs(Folder f) {
        return parsePositiveInt(f.hasKey(PAYMENT_TIMEOUT_MS) ? f.get(PAYMENT_TIMEOUT_MS) : null, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Best-effort mode detection. SDKs vary; we map known signals to a small enum
     * so the posture layer stays testable without mocking WebSockets.
     */
    static SdkMode detectMode(Kiponos kiponos) {
        try {
            // Prefer explicit API if present via reflection-friendly path
            if (kiponos.getRootFolder() != null) {
                return SdkMode.LIVE;
            }
            return SdkMode.SAFE;
        } catch (Exception e) {
            return SdkMode.OFFLINE;
        }
    }

    /**
     * Pure policy: which timeout do we serve, and where did it come from?
     *
     * LIVE  → use hub value
     * OFFLINE / LKG → use last known good (passed in as hubValue when cache still holds it)
     * SAFE → fail closed to a conservative default
     */
    static ReadPosture resolvePosture(SdkMode mode, int hubOrLkgValue, int safeDefault) {
        return switch (mode) {
            case LIVE -> new ReadPosture(mode, Math.max(1, hubOrLkgValue), "live-hub");
            case OFFLINE, LKG -> new ReadPosture(SdkMode.LKG, Math.max(1, hubOrLkgValue), "last-known-good");
            case SAFE -> new ReadPosture(mode, Math.max(1, safeDefault), "safe-default");
        };
    }

    static int parsePositiveInt(String raw, int def) {
        if (raw == null || raw.isBlank()) return def;
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    enum SdkMode { LIVE, LKG, OFFLINE, SAFE }

    record ReadPosture(SdkMode mode, int effectiveTimeoutMs, String source) {
        String summaryLine() {
            return switch (mode) {
                case LIVE -> "POSTURE: LIVE — serving hub timeout " + effectiveTimeoutMs + "ms";
                case LKG, OFFLINE -> "POSTURE: LKG — hub unreachable; serving last known " + effectiveTimeoutMs + "ms";
                case SAFE -> "POSTURE: SAFE — fail-closed default " + effectiveTimeoutMs + "ms";
            };
        }
    }
}
