package io.kiponos.examples.multienv;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Standalone Java main — multi-environment profile.
 *
 * Classic pain: the same fat jar ships everywhere, but someone copied
 * {@code application-staging.yml} onto prod (or the wrong env file won the merge).
 *
 * Kiponos fix: the process identity is the {@code -Dkiponos=...} profile path.
 * Same binary. Different hub profile. Env-specific keys live under that profile.
 */
public final class MultiEnvProfileApp {

    private static final String EXAMPLES = "examples";
    private static final String MULTI_ENV = "multi-env";
    private static final String API_BASE_URL = "api-base-url";
    private static final String ENV_LABEL = "env-label";

    public static void main(String[] args) throws InterruptedException {
        String profile = System.getProperty("kiponos", "(unset)");
        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder folder = ensureMultiEnvFolder(kiponos, profile);
            String envLabel = readEnvLabel(folder);
            String apiBase = readApiBaseUrl(folder);

            System.out.println("========================================");
            System.out.println("  Kiponos example: multi-env profile");
            System.out.println("  -Dkiponos profile: " + profile);
            System.out.println("  folder: examples / multi-env");
            System.out.println("  env-label:     " + envLabel);
            System.out.println("  api-base-url:  " + apiBase);
            System.out.println("========================================");
            System.out.println("Same jar. Different profile. No env-file roulette.");
            System.out.println("Point another process at staging or prod with a different -Dkiponos=...");

            Thread.sleep(3_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    /**
     * Ensure demo keys exist. Defaults are safe for a first-run demo on whatever
     * profile you connected; real teams set distinct values per profile in the hub.
     */
    static Folder ensureMultiEnvFolder(Kiponos kiponos, String profileHint) {
        Folder root = kiponos.getRootFolder();
        Folder examples = root.folderOrCreate(EXAMPLES);
        Folder multiEnv = examples.folderOrCreate(MULTI_ENV);

        String inferred = inferEnvLabel(profileHint);
        if (!multiEnv.hasKey(ENV_LABEL)) {
            multiEnv.set(ENV_LABEL, inferred);
            System.out.println("Created default key env-label=" + inferred + " (first run).");
        }
        if (!multiEnv.hasKey(API_BASE_URL)) {
            String defaultUrl = defaultApiBaseFor(inferred);
            multiEnv.set(API_BASE_URL, defaultUrl);
            System.out.println("Created default key api-base-url=" + defaultUrl + " (first run).");
        }
        return multiEnv;
    }

    static String readEnvLabel(Folder multiEnv) {
        if (!multiEnv.hasKey(ENV_LABEL)) {
            return "unknown";
        }
        String raw = multiEnv.get(ENV_LABEL);
        return raw == null || raw.isBlank() ? "unknown" : raw.trim();
    }

    static String readApiBaseUrl(Folder multiEnv) {
        if (!multiEnv.hasKey(API_BASE_URL)) {
            return "https://api.example.invalid";
        }
        String raw = multiEnv.get(API_BASE_URL);
        return raw == null || raw.isBlank() ? "https://api.example.invalid" : raw.trim();
    }

    /** Pure helper: map profile path text → coarse env label (no SDK). */
    static String inferEnvLabel(String profile) {
        if (profile == null) {
            return "dev";
        }
        String p = profile.toLowerCase();
        if (p.contains("'prod'") || p.contains("[\"prod\"]") || p.contains("/prod/") || p.contains("prod']")) {
            return "prod";
        }
        if (p.contains("stag") || p.contains("stage") || p.contains("uat")) {
            return "staging";
        }
        if (p.contains("prod")) {
            // bare "prod" token in path segments
            return "prod";
        }
        return "dev";
    }

    static String defaultApiBaseFor(String envLabel) {
        return switch (envLabel == null ? "dev" : envLabel.toLowerCase()) {
            case "prod", "production" -> "https://api.example.com";
            case "staging", "stage", "uat" -> "https://api.staging.example.com";
            default -> "https://api.dev.example.com";
        };
    }

    private MultiEnvProfileApp() {
    }
}
