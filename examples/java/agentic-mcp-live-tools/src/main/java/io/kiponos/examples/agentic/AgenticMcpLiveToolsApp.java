package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * MCP tools that read a live hub — no host restart
 * Hub leaf: examples/agentic-mcp-live-tools/tools-allow (default search,read)
 * Pain: Restarting Grok Build / Cursor / Claude Code MCP just to flip a write tool
 */
public final class AgenticMcpLiveToolsApp {
    public static final String KEY = "tools-allow";
    public static final String DEFAULT = "search,read";
    public static final String FOLDER = "agentic-mcp-live-tools";

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

    private AgenticMcpLiveToolsApp() {}
}
