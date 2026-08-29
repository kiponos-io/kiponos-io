package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.data.ConfigValUpdatedResponse;
import io.kiponos.sdk.configs.Folder;

/**
 * Claude Code Finished the Turn Blind — MCP write tool gated live on the Mirror Phone Path
 * Product: mirror-phone · Agent host: Claude Code
 * Hub leaf: examples/agentic-dev-0906-am-mcp-gate/tools-allow (default search,read)
 * Pain: Restarting Grok Build / Cursor / Claude Code MCP just to flip a write tool
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticDev0906AmMcpGateApp {
    public static final String KEY = "tools-allow";
    public static final String DEFAULT = "search,read";
    public static final String FOLDER = "agentic-dev-0906-am-mcp-gate";

    public record Decision(String value, String action, boolean proceed) {}

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            k.afterValueUpdated((ConfigValUpdatedResponse ev) -> {
                if (ev == null || ev.getKey() == null) { return; }
                // live leaf examples/agentic-dev-0906-am-mcp-gate — next decide() is in-memory, no MCP restart
            });
            String v = args.length > 0 ? args[0] : read(p, KEY, DEFAULT);
            Decision d = decide(v);
            System.out.println("examples/" + FOLDER + "/" + KEY + "=" + d.value());
            System.out.println("action=" + d.action() + " proceed=" + d.proceed());
            // next MCP / tool call sees dashboard edits — no host restart
            Thread.sleep(400L);
        } finally {
            k.disconnect();
        }
    }

    public static Decision decide(String raw) {
        String v = norm(raw);
        boolean write = false;
        for (String p : v.split(",")) {
            if ("write".equalsIgnoreCase(p.trim())) {
                write = true;
                break;
            }
        }
        boolean proceed = !write;
        return new Decision(v, proceed ? "allow_listed_tools" : "deny_write_no_mcp_restart", proceed);
    }

    static String norm(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        return raw.trim();
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

    private AgenticDev0906AmMcpGateApp() {}
}
