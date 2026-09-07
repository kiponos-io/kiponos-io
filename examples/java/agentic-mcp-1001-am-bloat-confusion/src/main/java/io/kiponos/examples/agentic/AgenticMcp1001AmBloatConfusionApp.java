package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * AWS Named the Two Failures: Bloat and Confusion. Both Are Live Leaves
 * Product: senses · Agent host: MCP host
 * Hub leaf: examples/agentic-mcp-1001-am-bloat-confusion/schema-budget (default 4000)
 * Pain: MCP schemas ate the window before the user spoke
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMcp1001AmBloatConfusionApp {
    public static final String KEY = "schema-budget";
    public static final String DEFAULT = "4000";
    public static final String FOLDER = "agentic-mcp-1001-am-bloat-confusion";

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
        int cap;
        try { cap = Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { cap = 4000; }
        boolean okb = cap > 0;
        return new Decision(Integer.toString(cap), okb ? "schema_within_budget" : "schema_dump_blocked", okb);
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

    private AgenticMcp1001AmBloatConfusionApp() {}
}
