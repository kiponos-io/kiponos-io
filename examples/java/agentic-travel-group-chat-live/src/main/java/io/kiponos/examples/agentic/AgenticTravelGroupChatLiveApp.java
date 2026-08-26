package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Travel Coordinator App — live group-chat mute leaf
 * Hub leaf: examples/agentic-travel-group-chat-live/chat-mute
 * Default: none
 * Pain: mute a flooding channel without killing the agent / MCP session
 */
public final class AgenticTravelGroupChatLiveApp {
    public static final String KEY = "chat-mute";
    public static final String DEFAULT = "none";
    public static final String FOLDER = "agentic-travel-group-chat-live";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
            System.out.println("examples/" + FOLDER + "/" + KEY + "=" + v);
            // hot path: local get — write-tools honor mute without MCP reboot
            Thread.sleep(1200L);
        } finally {
            k.disconnect();
        }
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

    private AgenticTravelGroupChatLiveApp() {}
}
