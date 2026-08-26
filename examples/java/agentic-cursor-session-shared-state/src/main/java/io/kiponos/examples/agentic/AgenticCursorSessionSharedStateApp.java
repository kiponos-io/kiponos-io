package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Cursor (or peer agent) session posture shared on the hub
 * Hub leaf: examples/agentic-cursor-session-shared-state/session-posture
 * Default: focus=admin-wall,shopping-pause=off
 * Pain: second agent cannot inherit live posture without paste/restart
 */
public final class AgenticCursorSessionSharedStateApp {
    public static final String KEY = "session-posture";
    public static final String DEFAULT = "focus=admin-wall,shopping-pause=off";
    public static final String FOLDER = "agentic-cursor-session-shared-state";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
            System.out.println("examples/" + FOLDER + "/" + KEY + "=" + v);
            // hot path: local get — Claude Code / Grok Build peers read same leaf live
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

    private AgenticCursorSessionSharedStateApp() {}
}
