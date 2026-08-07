package io.kiponos.examples.sdkpeers;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Incident Pause That Freezes Risky UI Paths Instantly
 * Hub: incident/pause-risky (default off)
 * Pain: incident leads still ship SPA builds to hide buttons mid-fire
 * Peers: Java + Python + @kiponos/react (Node server)
 */
public final class ReactSdkIncidentPauseApp {
    public static final String FOLDER = "incident";
    public static final String KEY = "pause-risky";
    public static final String DEFAULT = "off";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
System.out.println(KEY + "=" + v);
// hot path: honor live incident pause for risky UI paths without restart
Thread.sleep(1200L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder()
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

    private ReactSdkIncidentPauseApp() {}
}
