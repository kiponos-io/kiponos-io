package io.kiponos.examples.hooks;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import io.kiponos.sdk.data.ConfigValUpdatedResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone Java main — react to dashboard value updates via hooks.
 *
 * Classic pain: ops changes a rate limit (or any live knob) and the only ways
 * the process notices are polling a file, catching SIGHUP, or restarting.
 *
 * Kiponos fix: register {@code afterValueUpdated}. When the hub edits a key,
 * this long-running process applies the new value without a restart or poll loop.
 */
public final class HooksValueUpdatedApp {

    static final String EXAMPLES = "examples";
    static final String HOOKS_VALUE_UPDATED = "hooks-value-updated";
    static final String MAX_RPS = "max-rps";

    static final int DEFAULT_MAX_RPS = 50;

    /** How long the demo stays connected so you can flip the dashboard. */
    static final long LISTEN_SECONDS = 45L;

    public static void main(String[] args) throws InterruptedException {
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        AtomicInteger liveMaxRps = new AtomicInteger(DEFAULT_MAX_RPS);
        CountDownLatch firstHook = new CountDownLatch(1);

        try {
            Folder folder = ensureHooksFolder(kiponos);
            int initial = readMaxRps(folder);
            liveMaxRps.set(initial);

            kiponos.afterValueUpdated(event -> onValueUpdated(event, liveMaxRps, firstHook));

            System.out.println("========================================");
            System.out.println("  Kiponos example: afterValueUpdated hooks");
            System.out.println("  folder: examples / hooks-value-updated");
            System.out.println("  key:    max-rps = " + liveMaxRps.get());
            System.out.println("========================================");
            System.out.println("Listening for hub edits (no poll, no SIGHUP).");
            System.out.println("In the dashboard, change max-rps — this process reacts live.");
            System.out.println("Listening up to " + LISTEN_SECONDS + "s…");

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LISTEN_SECONDS);
            while (System.nanoTime() < deadline) {
                // Demo "work loop": each tick reports the live limit the hook keeps warm.
                System.out.println("[worker] accepting traffic under max-rps=" + liveMaxRps.get());
                Thread.sleep(5_000L);
            }

            if (firstHook.getCount() == 0) {
                System.out.println("Hook fired at least once during the listen window.");
            } else {
                System.out.println("No hub edit observed this run — flip max-rps next time while listening.");
            }
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    /**
     * Hook body (also unit-testable via pure helpers). Filters to our demo key,
     * parses the new value, and updates process-local posture.
     */
    static void onValueUpdated(
            ConfigValUpdatedResponse event,
            AtomicInteger liveMaxRps,
            CountDownLatch firstHook
    ) {
        if (event == null || event.getKey() == null) {
            return;
        }
        if (!isMaxRpsKey(event.getKey())) {
            return;
        }
        int next = parseMaxRps(event.getValue());
        int prev = liveMaxRps.getAndSet(next);
        System.out.println("----------------------------------------");
        System.out.println("[hook] afterValueUpdated");
        System.out.println("  key:      " + event.getKey());
        System.out.println("  basePath: " + nullToEmpty(event.getBasePath()));
        System.out.println("  value:    " + nullToEmpty(event.getValue()));
        System.out.println("  applied:  max-rps " + prev + " → " + next);
        System.out.println("----------------------------------------");
        firstHook.countDown();
    }

    static Folder ensureHooksFolder(Kiponos kiponos) {
        Folder root = kiponos.getRootFolder();
        if (root == null) {
            throw new IllegalStateException(
                    "Kiponos root is null — SDK not Ready. Check KIPONOS_ID / KIPONOS_ACCESS / -Dkiponos profile.");
        }
        Folder examples = root.folderOrCreate(EXAMPLES);
        Folder hooks = examples.folderOrCreate(HOOKS_VALUE_UPDATED);
        if (!hooks.hasKey(MAX_RPS)) {
            hooks.set(MAX_RPS, String.valueOf(DEFAULT_MAX_RPS));
            System.out.println("Created default key max-rps=" + DEFAULT_MAX_RPS + " (first run).");
        }
        return hooks;
    }

    static int readMaxRps(Folder hooks) {
        if (!hooks.hasKey(MAX_RPS)) {
            return DEFAULT_MAX_RPS;
        }
        return parseMaxRps(hooks.get(MAX_RPS));
    }

    /** Pure helper: which key names this demo cares about. */
    static boolean isMaxRpsKey(String key) {
        return MAX_RPS.equalsIgnoreCase(key == null ? "" : key.trim());
    }

    /** Pure helper: hub string → positive RPS (fallback default). */
    static int parseMaxRps(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MAX_RPS;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : DEFAULT_MAX_RPS;
        } catch (NumberFormatException ex) {
            return DEFAULT_MAX_RPS;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private HooksValueUpdatedApp() {
    }
}
