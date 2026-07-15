package io.kiponos.examples.sre;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Standalone Java main — SRE degradation mode ("read-only mode" button).
 *
 * Classic pain: dependency is limping, disk is full, or a partner is on fire —
 * and the only way to put the service into a safer posture is a redeploy of
 * flags scattered across YAML.
 *
 * Kiponos fix: ops flips a small kill-switch tree in the dashboard:
 * {@code mode}, {@code accept-writes}, {@code background-jobs}. This process
 * reads the tree and prints the live posture — no jar rebuild.
 */
public final class DegradationModeApp {

    static final String EXAMPLES = "examples";
    static final String DEGRADATION = "sre-degradation-mode";

    static final String MODE = "mode";
    static final String ACCEPT_WRITES = "accept-writes";
    static final String BACKGROUND_JOBS = "background-jobs";

    /** full | read-only | maintenance */
    static final String DEFAULT_MODE = "full";

    public static void main(String[] args) throws InterruptedException {
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder folder = ensureDegradationFolder(kiponos);
            Posture posture = readPosture(folder);

            System.out.println("========================================");
            System.out.println("  Kiponos example: SRE degradation mode");
            System.out.println("  folder: examples / sre-degradation-mode");
            System.out.println("  mode:              " + posture.mode());
            System.out.println("  accept-writes:     " + posture.acceptWrites());
            System.out.println("  background-jobs:   " + posture.backgroundJobs());
            System.out.println("========================================");
            System.out.println(posture.summaryLine());
            System.out.println();
            System.out.println("Ops play: set mode to \"read-only\" or \"maintenance\"");
            System.out.println("in the Kiponos dashboard, then re-run — no redeploy.");

            // Brief window so logs / dashboard observers see a live session
            Thread.sleep(3_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureDegradationFolder(Kiponos kiponos) {
        Folder root = kiponos.getRootFolder();
        if (root == null) {
            throw new IllegalStateException(
                    "Kiponos root is null — SDK not Ready. Check KIPONOS_ID / KIPONOS_ACCESS / -Dkiponos profile.");
        }
        Folder examples = root.folderOrCreate(EXAMPLES);
        Folder degradation = examples.folderOrCreate(DEGRADATION);

        if (!degradation.hasKey(MODE)) {
            degradation.set(MODE, DEFAULT_MODE);
            System.out.println("Created default key mode=" + DEFAULT_MODE + " (first run).");
        }
        if (!degradation.hasKey(ACCEPT_WRITES)) {
            degradation.set(ACCEPT_WRITES, "yes");
            System.out.println("Created default key accept-writes=yes (first run).");
        }
        if (!degradation.hasKey(BACKGROUND_JOBS)) {
            degradation.set(BACKGROUND_JOBS, "yes");
            System.out.println("Created default key background-jobs=yes (first run).");
        }
        return degradation;
    }

    /**
     * Resolve effective posture from the kill-switch tree.
     *
     * {@code mode} is the primary SRE button:
     * <ul>
     *   <li>{@code full} — honor accept-writes / background-jobs knobs</li>
     *   <li>{@code read-only} — force writes off; jobs follow their knob</li>
     *   <li>{@code maintenance} — force writes and jobs off</li>
     * </ul>
     */
    static Posture readPosture(Folder degradation) {
        String mode = normalizeMode(degradation.hasKey(MODE) ? degradation.get(MODE) : DEFAULT_MODE);
        boolean knobWrites = parseTruth(degradation.hasKey(ACCEPT_WRITES)
                ? degradation.get(ACCEPT_WRITES) : "yes");
        boolean knobJobs = parseTruth(degradation.hasKey(BACKGROUND_JOBS)
                ? degradation.get(BACKGROUND_JOBS) : "yes");

        return resolvePosture(mode, knobWrites, knobJobs);
    }

    /** Pure helper: mode + knobs → effective service posture. */
    static Posture resolvePosture(String mode, boolean knobWrites, boolean knobJobs) {
        String m = normalizeMode(mode);
        boolean acceptWrites;
        boolean backgroundJobs;

        switch (m) {
            case "read-only":
                acceptWrites = false;
                backgroundJobs = knobJobs;
                break;
            case "maintenance":
                acceptWrites = false;
                backgroundJobs = false;
                break;
            case "full":
            default:
                acceptWrites = knobWrites;
                backgroundJobs = knobJobs;
                break;
        }
        return new Posture(m, acceptWrites, backgroundJobs);
    }

    static String normalizeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MODE;
        }
        String m = raw.trim().toLowerCase().replace('_', '-');
        if ("readonly".equals(m) || "ro".equals(m)) {
            return "read-only";
        }
        if ("maint".equals(m) || "down".equals(m)) {
            return "maintenance";
        }
        if ("full".equals(m) || "read-only".equals(m) || "maintenance".equals(m)) {
            return m;
        }
        return DEFAULT_MODE;
    }

    static boolean parseTruth(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String v = raw.trim();
        return "yes".equalsIgnoreCase(v)
                || "true".equalsIgnoreCase(v)
                || "on".equalsIgnoreCase(v)
                || "1".equals(v);
    }

    /** Immutable snapshot of effective SRE posture. */
    static final class Posture {
        private final String mode;
        private final boolean acceptWrites;
        private final boolean backgroundJobs;

        Posture(String mode, boolean acceptWrites, boolean backgroundJobs) {
            this.mode = mode;
            this.acceptWrites = acceptWrites;
            this.backgroundJobs = backgroundJobs;
        }

        String mode() {
            return mode;
        }

        boolean acceptWrites() {
            return acceptWrites;
        }

        boolean backgroundJobs() {
            return backgroundJobs;
        }

        String summaryLine() {
            if ("maintenance".equals(mode)) {
                return "[posture] MAINTENANCE — refusing writes and background jobs (safe shell).";
            }
            if ("read-only".equals(mode)) {
                return "[posture] READ-ONLY — refusing mutations"
                        + (backgroundJobs ? "; background jobs still allowed." : "; background jobs off.");
            }
            if (!acceptWrites && !backgroundJobs) {
                return "[posture] FULL mode but knobs closed — no writes, no jobs (check accept-writes / background-jobs).";
            }
            if (!acceptWrites) {
                return "[posture] FULL mode — reads only (accept-writes=no); jobs="
                        + (backgroundJobs ? "on" : "off") + ".";
            }
            return "[posture] FULL — accepting writes; background jobs "
                    + (backgroundJobs ? "ON" : "OFF") + ".";
        }
    }

    private DegradationModeApp() {
    }
}
