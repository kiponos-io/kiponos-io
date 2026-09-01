package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.data.ConfigValUpdatedResponse;
import io.kiponos.sdk.configs.Folder;

/**
 * I Restarted Cursor to Flip Shopping incident pause as a leaf — The Shopping Wall Already Knew
 * Product: shopping · Agent host: Cursor
 * Hub leaf: examples/agentic-med-0926-mi-shopping-pause/incident-pause (default off)
 * Pain: Shopping freeze waited for an MCP restart
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMed0926MiShoppingPauseApp {
    public static final String KEY = "incident-pause";
    public static final String DEFAULT = "off";
    public static final String FOLDER = "agentic-med-0926-mi-shopping-pause";

    public record Decision(String value, String action, boolean proceed) {}

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            k.afterValueUpdated((ConfigValUpdatedResponse ev) -> {
                if (ev == null || ev.getKey() == null) { return; }
                // live leaf examples/agentic-med-0926-mi-shopping-pause — next decide() is in-memory, no MCP restart
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
        boolean paused = v.equalsIgnoreCase("on") || v.equalsIgnoreCase("paused")
                || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("true");
        return new Decision(v, paused ? "freeze_shopping_writes" : "shopping_path_live", !paused);
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

    private AgenticMed0926MiShoppingPauseApp() {}
}
