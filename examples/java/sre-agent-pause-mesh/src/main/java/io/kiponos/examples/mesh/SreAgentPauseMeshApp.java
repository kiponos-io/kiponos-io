package io.kiponos.examples.mesh;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Pause Heavy Agent Tools When SRE Enters Degradation Mode
 * Hub leaf: examples/sre-agent-pause-mesh/agent-tools (default full)
 * Pain: agents keep burning capacity while humans try to stabilize
 */
public final class SreAgentPauseMeshApp {
    public static final String KEY = "agent-tools";
    public static final String DEFAULT = "full";

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
                .folderOrCreate("sre-agent-pause-mesh");
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

    private SreAgentPauseMeshApp() {}
}
