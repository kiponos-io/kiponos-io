package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.data.ConfigValUpdatedResponse;
import io.kiponos.sdk.configs.Folder;

/**
 * The MCP Spec Author Said Clients Should Defer Tools. Mine Didn't. I Still Needed a Knob
 * Product: senses · Agent host: Claude Code
 * Hub leaf: examples/agentic-mcp-0918-am-client-blame/tool-search (default on)
 * Pain: Client dumped 100 MCP tools into context
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMcp0918AmClientBlameApp {
    public static final String KEY = "tool-search";
    public static final String DEFAULT = "on";
    public static final String FOLDER = "agentic-mcp-0918-am-client-blame";

    public record Decision(String value, String action, boolean proceed) {}

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            k.afterValueUpdated((ConfigValUpdatedResponse ev) -> {
                if (ev == null || ev.getKey() == null) { return; }
                // live leaf examples/agentic-mcp-0918-am-client-blame — next decide() is in-memory, no MCP restart
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
        boolean live = v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("live")
                || v.equalsIgnoreCase("on") || v.equalsIgnoreCase("true");
        return new Decision(v, live ? "defer_schemas" : "dump_all_schemas", live);
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

    private AgenticMcp0918AmClientBlameApp() {}
}
