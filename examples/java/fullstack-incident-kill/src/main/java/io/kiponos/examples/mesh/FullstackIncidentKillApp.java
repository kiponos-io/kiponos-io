package io.kiponos.examples.mesh;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Kill the Path Everywhere — JVM, Agent, React BFF, Angular Admin
 * Hub leaf: incident/path-enabled (default on)
 * Peers: Java + Python + @kiponos/react + @kiponos/angular — same Team profile tree
 */
public final class FullstackIncidentKillApp {
    public static final String KEY = "path-enabled";
    public static final String DEFAULT = "on";
    public static final String PATH_LABEL = "incident/path-enabled";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
    System.out.println(KEY + "=" + v);
            // hot path: every peer honors the same Team leaf without restart
            Thread.sleep(1200L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder()
        .folderOrCreate("incident");
        if (!f.hasKey(KEY)) {
            f.set(KEY, DEFAULT);
        }
        return f;
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) return def;
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    private FullstackIncidentKillApp() {}
}
