package io.kiponos.examples.mesh;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Agent Skills That Reload Without Killing the Session
 * Hub leaf: examples/agent-skill-live-reload/enabled-set (default research,notify)
 * Pain: agent restarts wipe context just to flip a skill flag
 */
public final class AgentSkillLiveReloadApp {
    public static final String KEY = "enabled-set";
    public static final String DEFAULT = "research,notify";

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
                .folderOrCreate("agent-skill-live-reload");
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

    private AgentSkillLiveReloadApp() {}
}
