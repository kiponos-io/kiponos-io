package io.kiponos.examples.mesh;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * MCP Tool Gates Without Restarting the Agent Host
 * Hub leaf: examples/mcp-tool-gate-live/tools-allow (default read,search)
 * Pain: dangerous tools stay hot until someone restarts the MCP host
 */
public final class McpToolGateLiveApp {
    public static final String KEY = "tools-allow";
    public static final String DEFAULT = "read,search";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
            System.out.println(KEY + "=" + v);
            // hot path: peers (Java/Python/React-Node/Angular-Node) share this leaf live
            Thread.sleep(1200L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder()
                .folderOrCreate("examples")
                .folderOrCreate("mcp-tool-gate-live");
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

    private McpToolGateLiveApp() {}
}
