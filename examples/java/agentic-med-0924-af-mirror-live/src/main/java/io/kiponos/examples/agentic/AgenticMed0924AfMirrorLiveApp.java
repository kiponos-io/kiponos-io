package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * I Restarted Grok Build to Flip Mirror Phone live device leaf — The Shopping Wall Already Knew
 * Product: shopping · Agent host: Grok Build
 * Hub leaf: examples/agentic-med-0924-af-mirror-live/device-live (default yes)
 * Pain: Mirror Phone live device was a rumor in chat
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMed0924AfMirrorLiveApp {
    public static final String KEY = "device-live";
    public static final String DEFAULT = "yes";
    public static final String FOLDER = "agentic-med-0924-af-mirror-live";

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
        boolean live = v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("live")
                || v.equalsIgnoreCase("on") || v.equalsIgnoreCase("true");
        return new Decision(v, live ? "mirror_device_live" : "route_other_mirror_device", live);
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

    private AgenticMed0924AfMirrorLiveApp() {}
}
