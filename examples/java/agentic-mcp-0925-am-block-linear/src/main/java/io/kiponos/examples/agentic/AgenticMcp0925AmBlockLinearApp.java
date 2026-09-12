package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.data.ConfigValUpdatedResponse;
import io.kiponos.sdk.configs.Folder;

/**
 * Block Rebuilt Linear MCP Three Times for the Same Schema Tax
 * Product: travel · Agent host: Cursor
 * Hub leaf: examples/agentic-mcp-0925-am-block-linear/result-cap (default 2000)
 * Pain: One MCP tool returned hundreds of thousands of tokens
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMcp0925AmBlockLinearApp {
    public static final String KEY = "result-cap";
    public static final String DEFAULT = "2000";
    public static final String FOLDER = "agentic-mcp-0925-am-block-linear";

    public record Decision(String value, String action, boolean proceed) {}

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            k.afterValueUpdated((ConfigValUpdatedResponse ev) -> {
                if (ev == null || ev.getKey() == null) { return; }
                // live leaf examples/agentic-mcp-0925-am-block-linear — next decide() is in-memory, no MCP restart
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
        int cap;
        try { cap = Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { cap = 2000; }
        boolean okb = cap > 0;
        return new Decision(Integer.toString(cap), okb ? "result_projected" : "result_too_fat", okb);
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

    private AgenticMcp0925AmBlockLinearApp() {}
}
