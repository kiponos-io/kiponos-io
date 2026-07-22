package io.kiponos.examples.aha;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/** Aha example: Session cache TTL live
 * Tree: examples / aha-cache-ttl / ttl-seconds
 */
public final class CacheTtlApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("ttl-seconds=" + read(p, "ttl-seconds", "300"));
            System.out.println("Session cache TTL live");
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-cache-ttl");
        if (!f.hasKey("ttl-seconds")) f.set("ttl-seconds", "300");
        return f;
    }
    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) return def;
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }
    static int readInt(Folder p, String key, int def) {
        try { return Integer.parseInt(read(p, key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }
    private CacheTtlApp() {}
}
