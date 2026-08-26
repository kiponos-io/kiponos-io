package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Senses as live hub leaves — mid-turn decisions
 * Hub leaf: examples/agentic-senses-mid-turn/priority (default P3)
 * Pain: Agent finishes a turn on stale network truth because the sense lived in a file
 */
public final class AgenticSensesMidTurnApp {
    public static final String KEY = "priority";
    public static final String DEFAULT = "P3";
    public static final String FOLDER = "agentic-senses-mid-turn";

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

    private AgenticSensesMidTurnApp() {}
}
