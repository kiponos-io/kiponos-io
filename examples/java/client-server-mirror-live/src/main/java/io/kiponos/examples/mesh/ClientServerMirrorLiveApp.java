package io.kiponos.examples.mesh;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Client Mirrors Server Truth — Without Putting Tokens in the Browser
 * Hub leaf: examples/client-server-mirror-live/status (default ok)
 * Pain: SPA and server disagree because the SPA was treated as a hub peer
 */
public final class ClientServerMirrorLiveApp {
    public static final String KEY = "status";
    public static final String DEFAULT = "ok";

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
                .folderOrCreate("client-server-mirror-live");
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

    private ClientServerMirrorLiveApp() {}
}
