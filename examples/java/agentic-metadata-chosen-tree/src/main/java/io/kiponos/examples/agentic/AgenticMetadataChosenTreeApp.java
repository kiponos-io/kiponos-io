package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Agents choose their own shared metadata tree
 * Hub leaf: examples/agentic-metadata-chosen-tree/owner-agent (default travel-coordinator)
 * Pain: Shared state pasted into chat because nobody owned a live folder
 */
public final class AgenticMetadataChosenTreeApp {
    public static final String KEY = "owner-agent";
    public static final String DEFAULT = "travel-coordinator";
    public static final String FOLDER = "agentic-metadata-chosen-tree";

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

    private AgenticMetadataChosenTreeApp() {}
}
