package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.data.ConfigValUpdatedResponse;
import io.kiponos.sdk.configs.Folder;

/**
 * MCP vs CLI vs Skills: The Internet Said MCP Is Dead. The Missing Piece Was a Hub
 * Product: travel · Agent host: Cursor
 * Hub leaf: examples/agentic-mcp-0912-pm-skill-vs-mcp/shared-truth (default live)
 * Pain: Agent frameworks shipped tools but not a live hub
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMcp0912PmSkillVsMcpApp {
    public static final String KEY = "shared-truth";
    public static final String DEFAULT = "live";
    public static final String FOLDER = "agentic-mcp-0912-pm-skill-vs-mcp";

    public record Decision(String value, String action, boolean proceed) {}

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            k.afterValueUpdated((ConfigValUpdatedResponse ev) -> {
                if (ev == null || ev.getKey() == null) { return; }
                // live leaf examples/agentic-mcp-0912-pm-skill-vs-mcp — next decide() is in-memory, no MCP restart
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
        boolean live = v.equalsIgnoreCase("live") || v.equalsIgnoreCase("yes")
                || v.equalsIgnoreCase("on");
        return new Decision(v, live ? "peers_share_live_hub" : "refuse_stale_host_argv", live);
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

    private AgenticMcp0912PmSkillVsMcpApp() {}
}
