package io.kiponos.examples.sdkpeers;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Feature Kill From the Hub — Angular Peer, Same Leaf as Java
 * Hub: flags/feature-x (default off)
 * Pain: Angular admin keeps offering a path after Java already killed it
 * Peers: Java + Python + @kiponos/angular (Node server)
 */
public final class AngularSdkFeatureKillApp {
    public static final String FOLDER = "flags";
    public static final String KEY = "feature-x";
    public static final String DEFAULT = "off";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
System.out.println(KEY + "=" + v);
// hot path: honor live Angular-adjacent feature kill flag without restart
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

    private AngularSdkFeatureKillApp() {}
}
