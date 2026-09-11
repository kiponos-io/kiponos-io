package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.data.ConfigValUpdatedResponse;
import io.kiponos.sdk.configs.Folder;

/**
 * The MCP Doom Loop: Fail, Retry, More Tokens, Worse Model, Repeat
 * Product: shopping · Agent host: Cursor
 * Hub leaf: examples/agentic-mcp-0917-am-doom-loop/retry-max (default 2)
 * Pain: MCP doom loop: fail, retry, more tokens, worse model
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMcp0917AmDoomLoopApp {
    public static final String KEY = "retry-max";
    public static final String DEFAULT = "2";
    public static final String FOLDER = "agentic-mcp-0917-am-doom-loop";

    public record Decision(String value, String action, boolean proceed) {}

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            k.afterValueUpdated((ConfigValUpdatedResponse ev) -> {
                if (ev == null || ev.getKey() == null) { return; }
                // live leaf examples/agentic-mcp-0917-am-doom-loop — next decide() is in-memory, no MCP restart
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
        catch (NumberFormatException e) { cap = 2; }
        boolean okb = cap > 0;
        return new Decision(Integer.toString(cap), okb ? "retry_within_cap" : "doom_loop_stopped", okb);
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

    private AgenticMcp0917AmDoomLoopApp() {}
}
