package io.kiponos.examples.retail;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Retail checkout PSP timeout — Black Friday edition.
 *
 * Classic pain: payment-service timeout is wrong under load, and the only
 * way to raise it is a redeploy during peak cart abandonment.
 *
 * Kiponos fix: live timeout-ms (and optional soft-fail flag) for the checkout
 * path, flipped from the hub without shipping a new jar to every storefront pod.
 */
public final class RetailCheckoutTimeoutApp {

    static final String EXAMPLES = "examples";
    static final String FOLDER = "retail-checkout-timeout";

    static final String TIMEOUT_MS = "timeout-ms";
    static final String SOFT_FAIL = "soft-fail-on-timeout";

    static final int DEFAULT_TIMEOUT_MS = 3_000;

    public static void main(String[] args) throws InterruptedException {
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder folder = ensureFolder(kiponos);
            CheckoutPolicy policy = readPolicy(folder);

            System.out.println("========================================");
            System.out.println("  Kiponos example: retail checkout timeout");
            System.out.println("  folder: examples / retail-checkout-timeout");
            System.out.println("  timeout-ms:          " + policy.timeoutMs());
            System.out.println("  soft-fail-on-timeout:" + policy.softFailOnTimeout());
            System.out.println("========================================");
            System.out.println(policy.summaryLine());
            System.out.println();
            System.out.println("Ops play: raise timeout-ms during PSP lag,");
            System.out.println("or enable soft-fail so carts retry instead of 500.");

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
        if (!f.hasKey(TIMEOUT_MS)) {
            f.set(TIMEOUT_MS, String.valueOf(DEFAULT_TIMEOUT_MS));
            System.out.println("Created default timeout-ms=" + DEFAULT_TIMEOUT_MS);
        }
        if (!f.hasKey(SOFT_FAIL)) {
            f.set(SOFT_FAIL, "no");
            System.out.println("Created default soft-fail-on-timeout=no");
        }
        return f;
    }

    static CheckoutPolicy readPolicy(Folder f) {
        int timeout = parsePositiveInt(f.hasKey(TIMEOUT_MS) ? f.get(TIMEOUT_MS) : null, DEFAULT_TIMEOUT_MS);
        boolean soft = parseTruth(f.hasKey(SOFT_FAIL) ? f.get(SOFT_FAIL) : "no");
        return resolvePolicy(timeout, soft);
    }

    static CheckoutPolicy resolvePolicy(int timeoutMs, boolean softFailOnTimeout) {
        int t = Math.min(Math.max(timeoutMs, 250), 60_000); // clamp 250ms..60s
        return new CheckoutPolicy(t, softFailOnTimeout);
    }

    static boolean parseTruth(String raw) {
        if (raw == null) return false;
        String s = raw.trim().toLowerCase();
        return s.equals("yes") || s.equals("true") || s.equals("on") || s.equals("1");
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

    record CheckoutPolicy(int timeoutMs, boolean softFailOnTimeout) {
        String summaryLine() {
            if (softFailOnTimeout) {
                return "CHECKOUT: timeout=" + timeoutMs + "ms, soft-fail ON (retry path on PSP lag)";
            }
            return "CHECKOUT: timeout=" + timeoutMs + "ms, hard fail on PSP timeout";
        }
    }
}
