package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * The live hub agent frameworks do not ship
 * Hub leaf: examples/agentic-frameworks-missing-hub/shared-truth (default live)
 * Pain: Tools and MCP without a live shared tree still force restarts
 */
public final class AgenticFrameworksMissingHubApp {
    public static final String KEY = "shared-truth";
    public static final String DEFAULT = "live";
    public static final String FOLDER = "agentic-frameworks-missing-hub";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
            System.out.println("examples/" + FOLDER + "/" + KEY + "=" + v);
            // hot path: local get — next MCP/tool call sees dashboard edits without restart
            Thread.sleep(1200L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder()
                .folderOrCreate("examples")
                .folderOrCreate(FOLDER);
        if (!f.hasKey(KEY)) {
            f.set(KEY, DEFAULT);
        }
        return f;
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) {
            return def;
        }
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    private AgenticFrameworksMissingHubApp() {}
}
