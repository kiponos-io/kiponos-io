package io.kiponos.examples.aha;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/** Aha example: Root log level live
 * Tree: examples / aha-log-levels / level
 */
public final class LogLevelsApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("level=" + read(p, "level", "INFO"));
            System.out.println("Root log level live");
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-log-levels");
        if (!f.hasKey("level")) f.set("level", "INFO");
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
    private LogLevelsApp() {}
}
