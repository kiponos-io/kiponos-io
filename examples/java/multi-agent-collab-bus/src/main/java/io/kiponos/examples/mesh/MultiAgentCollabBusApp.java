package io.kiponos.examples.mesh;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Agents That Hand Off Work Without a Restart Handshake
 * Hub leaf: examples/multi-agent-collab-bus/handoff-ticket (default idle)
 * Pain: multi-agent pipelines freeze on process restarts and file drops
 */
public final class MultiAgentCollabBusApp {
    public static final String KEY = "handoff-ticket";
    public static final String DEFAULT = "idle";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
            System.out.println(KEY + "=" + v);
            // hot path: peers (Java/Python/React-Node/Angular-Node) share this leaf live
            Thread.sleep(1200L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder()
                .folderOrCreate("examples")
                .folderOrCreate("multi-agent-collab-bus");
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

    private MultiAgentCollabBusApp() {}
}
