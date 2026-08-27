package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * I Restarted Cursor to Flip Sense priority mid-turn — The Travel Wall Already Knew
 * Product: travel · Agent host: Cursor
 * Hub leaf: examples/agentic-med-0902-mi-sense-priority/priority (default P3)
 * Pain: Agent finishes a turn on stale sense truth because the probe lived in a file
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMed0902MiSensePriorityApp {
    public static final String KEY = "priority";
    public static final String DEFAULT = "P3";
    public static final String FOLDER = "agentic-med-0902-mi-sense-priority";

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
        String u = v.toUpperCase();
        boolean abort = u.startsWith("P0") || u.startsWith("P1");
        return new Decision(v, abort ? "abort_mid_turn_no_restart" : "continue_turn", !abort);
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

    private AgenticMed0902MiSensePriorityApp() {}
}
