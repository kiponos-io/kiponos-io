package io.kiponos.examples.springrest;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import org.springframework.stereotype.Component;

/**
 * Reads operational knobs from the Kiponos hub on demand (hot path = local cache).
 *
 * Tree:
 * <pre>
 * examples / spring-boot-rest / request-timeout-ms
 * examples / spring-boot-rest / greeting
 * </pre>
 */
@Component
public class LiveOpsConfig {

    static final String EXAMPLES = "examples";
    static final String SPRING_BOOT_REST = "spring-boot-rest";
    static final String REQUEST_TIMEOUT_MS = "request-timeout-ms";
    static final String GREETING = "greeting";

    static final int DEFAULT_TIMEOUT_MS = 3_000;
    static final String DEFAULT_GREETING = "Hello from live Kiponos config";

    private final Kiponos kiponos;
    private volatile boolean ensured;

    public LiveOpsConfig(Kiponos kiponos) {
        this.kiponos = kiponos;
    }

    /**
     * Ensure demo keys exist (first run defaults). Safe to call from request path.
     */
    public Folder ensureOpsFolder() {
        Folder root = kiponos.getRootFolder();
        if (root == null) {
            throw new IllegalStateException(
                    "Kiponos root is null — SDK not Ready. Check KIPONOS_ID / KIPONOS_ACCESS / -Dkiponos profile.");
        }
        Folder examples = root.folderOrCreate(EXAMPLES);
        Folder ops = examples.folderOrCreate(SPRING_BOOT_REST);
        if (!ops.hasKey(REQUEST_TIMEOUT_MS)) {
            ops.set(REQUEST_TIMEOUT_MS, String.valueOf(DEFAULT_TIMEOUT_MS));
            System.out.println("Created default key request-timeout-ms=" + DEFAULT_TIMEOUT_MS + " (first run).");
        }
        if (!ops.hasKey(GREETING)) {
            ops.set(GREETING, DEFAULT_GREETING);
            System.out.println("Created default key greeting=... (first run).");
        }
        ensured = true;
        return ops;
    }

    public int requestTimeoutMs() {
        Folder ops = opsFolder();
        return parseTimeoutMs(ops.hasKey(REQUEST_TIMEOUT_MS) ? ops.get(REQUEST_TIMEOUT_MS) : null);
    }

    public String greeting() {
        Folder ops = opsFolder();
        return parseGreeting(ops.hasKey(GREETING) ? ops.get(GREETING) : null);
    }

    private Folder opsFolder() {
        if (!ensured) {
            return ensureOpsFolder();
        }
        Folder root = kiponos.getRootFolder();
        if (root == null) {
            throw new IllegalStateException("Kiponos root is null — SDK not Ready.");
        }
        return root.folderOrCreate(EXAMPLES).folderOrCreate(SPRING_BOOT_REST);
    }

    /** Pure helper: string from hub → timeout ms (no SDK). */
    static int parseTimeoutMs(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TIMEOUT_MS;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : DEFAULT_TIMEOUT_MS;
        } catch (NumberFormatException ex) {
            return DEFAULT_TIMEOUT_MS;
        }
    }

    /** Pure helper: string from hub → greeting (no SDK). */
    static String parseGreeting(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_GREETING;
        }
        return raw.trim();
    }
}
