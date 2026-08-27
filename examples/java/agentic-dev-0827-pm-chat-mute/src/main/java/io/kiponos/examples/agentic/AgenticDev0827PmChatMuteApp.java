package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * MCP host Finished the Turn Blind — Group-chat mute without host kill on the Shopping Path
 * Product: shopping · Agent host: MCP host
 * Hub leaf: examples/agentic-dev-0827-pm-chat-mute/chat-mute (default none)
 * Pain: Killing the agent host to stop a flooded travel group chat
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticDev0827PmChatMuteApp {
    public static final String KEY = "chat-mute";
    public static final String DEFAULT = "none";
    public static final String FOLDER = "agentic-dev-0827-pm-chat-mute";

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
        boolean muted = !(v.equalsIgnoreCase("none") || v.equalsIgnoreCase("off")
                || v.equalsIgnoreCase(""));
        return new Decision(v, muted ? "mute_sends_keep_session" : "group_chat_sends_live", !muted);
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

    private AgenticDev0827PmChatMuteApp() {}
}
