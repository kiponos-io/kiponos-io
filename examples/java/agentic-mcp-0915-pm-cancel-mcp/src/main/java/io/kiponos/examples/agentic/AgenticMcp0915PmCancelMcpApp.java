package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * The Long-Running MCP Task Had No Cancel. I Killed the Host
 * Product: senses · Agent host: Grok Build
 * Hub leaf: examples/agentic-mcp-0915-pm-cancel-mcp/cancel-token (default off)
 * Pain: Long-running MCP task had no cancel except kill the host
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMcp0915PmCancelMcpApp {
    public static final String KEY = "cancel-token";
    public static final String DEFAULT = "off";
    public static final String FOLDER = "agentic-mcp-0915-pm-cancel-mcp";

    public record Decision(String value, String action, boolean proceed) {}

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
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
        boolean paused = v.equalsIgnoreCase("on") || v.equalsIgnoreCase("paused")
                || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("true");
        return new Decision(v, paused ? "cancel_mcp_task" : "mcp_task_live", !paused);
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

    private AgenticMcp0915PmCancelMcpApp() {}
}
