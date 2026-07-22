package io.kiponos.examples.aha;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/** Aha example: Read HTTP client timeout live
 * Tree: examples / aha-http-timeout / timeout-ms
 */
public final class HttpTimeoutApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("timeout-ms=" + read(p, "timeout-ms", "3000"));
            System.out.println("Read HTTP client timeout live");
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-http-timeout");
        if (!f.hasKey("timeout-ms")) f.set("timeout-ms", "3000");
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
    private HttpTimeoutApp() {}
}
