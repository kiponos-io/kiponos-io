package io.kiponos.examples.killswitch;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Standalone Java main — the 3am kill switch.
 *
 * Classic pain: a flaky dependency is burning production and the only "fix"
 * is a redeploy of a boolean in application.yml.
 *
 * Kiponos fix: ops flips {@code payments-enabled} in the dashboard; this process
 * reads it live (and can react on the next check loop without a restart).
 */
public final class KillSwitchApp {

    private static final String EXAMPLES = "examples";
    private static final String KILL_SWITCH = "kill-switch";
    private static final String PAYMENTS_ENABLED = "payments-enabled";

    public static void main(String[] args) throws InterruptedException {
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder switchFolder = ensureKillSwitchFolder(kiponos);
            boolean enabled = readPaymentsEnabled(switchFolder);

            System.out.println("========================================");
            System.out.println("  Kiponos example: 3am kill switch");
            System.out.println("  folder: examples / kill-switch");
            System.out.println("  key:    payments-enabled = " + enabled);
            System.out.println("========================================");

            if (enabled) {
                System.out.println("[payments] ENABLED — processing orders (demo).");
                System.out.println("Flip payments-enabled to \"no\" in the Kiponos dashboard,");
                System.out.println("then re-run — no code change, no redeploy.");
            } else {
                System.out.println("[payments] DISABLED — refusing new charges (safe posture).");
                System.out.println("This is the 3am move: stop the bleeding from the hub.");
            }

            // Brief window so logs / dashboard observers see a live session
            Thread.sleep(3_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureKillSwitchFolder(Kiponos kiponos) {
        Folder root = kiponos.getRootFolder();
        Folder examples = root.folderOrCreate(EXAMPLES);
        Folder killSwitch = examples.folderOrCreate(KILL_SWITCH);
        if (!killSwitch.hasKey(PAYMENTS_ENABLED)) {
            // First run: default to enabled so demos are not brick-walled
            killSwitch.set(PAYMENTS_ENABLED, "yes");
            System.out.println("Created default key payments-enabled=yes (first run).");
        }
        return killSwitch;
    }

    static boolean readPaymentsEnabled(Folder killSwitch) {
        if (!killSwitch.hasKey(PAYMENTS_ENABLED)) {
            return true;
        }
        String raw = killSwitch.get(PAYMENTS_ENABLED);
        return "yes".equalsIgnoreCase(raw)
                || "true".equalsIgnoreCase(raw)
                || "on".equalsIgnoreCase(raw)
                || "1".equals(raw);
    }

    private KillSwitchApp() {
    }
}
