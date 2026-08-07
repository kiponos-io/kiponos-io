package io.kiponos.examples.sdkpeers;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Live Theme for Angular Admin Without an SPA Redeploy
 * Hub: ui/theme (default night)
 * Pain: admin theme tokens ship only with Angular builds
 * Peers: Java + Python + @kiponos/angular (Node server)
 */
public final class AngularSdkLiveThemeApp {
    public static final String FOLDER = "ui";
    public static final String KEY = "theme";
    public static final String DEFAULT = "night";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
System.out.println(KEY + "=" + v);
// hot path: honor live live Angular admin theme without restart
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

    private AngularSdkLiveThemeApp() {}
}
