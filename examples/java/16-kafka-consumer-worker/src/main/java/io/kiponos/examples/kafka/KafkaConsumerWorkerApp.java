package io.kiponos.examples.kafka;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Standalone Kafka-style consumer worker knobs.
 *
 * Classic pain: lag grows, poison messages pile up, and the only way to pause
 * a consumer group or change prefetch is a redeploy of application.yml.
 *
 * Kiponos fix: ops lives in a small tree — pause, prefetch, max-poll-records —
 * flipped in the dashboard without rebuilding the worker jar.
 */
public final class KafkaConsumerWorkerApp {

    static final String EXAMPLES = "examples";
    static final String FOLDER = "16-kafka-consumer-worker";

    static final String PAUSED = "paused";
    static final String PREFETCH = "prefetch";
    static final String MAX_POLL_RECORDS = "max-poll-records";

    static final int DEFAULT_PREFETCH = 500;
    static final int DEFAULT_MAX_POLL = 100;

    public static void main(String[] args) throws InterruptedException {
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder folder = ensureFolder(kiponos);
            ConsumerPolicy policy = readPolicy(folder);

            System.out.println("========================================");
            System.out.println("  Kiponos example: Kafka consumer worker");
            System.out.println("  folder: examples / 16-kafka-consumer-worker");
            System.out.println("  paused:            " + policy.paused());
            System.out.println("  prefetch:          " + policy.prefetch());
            System.out.println("  max-poll-records:  " + policy.maxPollRecords());
            System.out.println("========================================");
            System.out.println(policy.summaryLine());
            System.out.println();
            System.out.println("Ops play: set paused=yes during a poison storm,");
            System.out.println("or lower max-poll-records — no jar rebuild.");

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
        if (!f.hasKey(PAUSED)) {
            f.set(PAUSED, "no");
            System.out.println("Created default key paused=no (first run).");
        }
        if (!f.hasKey(PREFETCH)) {
            f.set(PREFETCH, String.valueOf(DEFAULT_PREFETCH));
            System.out.println("Created default key prefetch=" + DEFAULT_PREFETCH);
        }
        if (!f.hasKey(MAX_POLL_RECORDS)) {
            f.set(MAX_POLL_RECORDS, String.valueOf(DEFAULT_MAX_POLL));
            System.out.println("Created default key max-poll-records=" + DEFAULT_MAX_POLL);
        }
        return f;
    }

    static ConsumerPolicy readPolicy(Folder f) {
        boolean paused = parseTruth(f.hasKey(PAUSED) ? f.get(PAUSED) : "no");
        int prefetch = parsePositiveInt(f.hasKey(PREFETCH) ? f.get(PREFETCH) : null, DEFAULT_PREFETCH);
        int maxPoll = parsePositiveInt(f.hasKey(MAX_POLL_RECORDS) ? f.get(MAX_POLL_RECORDS) : null, DEFAULT_MAX_POLL);
        return resolvePolicy(paused, prefetch, maxPoll);
    }

    /** Pure: knobs → effective consumer policy. */
    static ConsumerPolicy resolvePolicy(boolean paused, int prefetch, int maxPollRecords) {
        int pref = Math.max(1, prefetch);
        int max = Math.max(1, maxPollRecords);
        // Prefetch should not be below poll batch; clamp for sanity
        if (pref < max) {
            pref = max;
        }
        return new ConsumerPolicy(paused, pref, max);
    }

    static boolean parseTruth(String raw) {
        if (raw == null) return false;
        String s = raw.trim().toLowerCase();
        return s.equals("yes") || s.equals("true") || s.equals("on") || s.equals("1") || s.equals("paused");
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

    record ConsumerPolicy(boolean paused, int prefetch, int maxPollRecords) {
        String summaryLine() {
            if (paused) {
                return "POSTURE: PAUSED — lag may grow; poison storm is frozen.";
            }
            return "POSTURE: RUNNING — prefetch=" + prefetch + ", max-poll-records=" + maxPollRecords;
        }
    }
}
