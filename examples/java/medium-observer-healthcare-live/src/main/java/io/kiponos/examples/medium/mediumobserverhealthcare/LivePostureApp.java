package io.kiponos.examples.medium.mediumobserverhealthcare;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Medium Super Pattern module: live posture for `active`.
 * Tree: examples / medium-observer-healthcare-live / active
 * The Observer Pattern Subscribe Topology Live — Healthcare War Room
 */
public final class LivePostureApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("========================================");
            System.out.println("  Kiponos Medium live posture");
            System.out.println("  example: medium-observer-healthcare-live");
            System.out.println("  active=" + read(p, "active", "baseline"));
            System.out.println("========================================");
            System.out.println("Change the hub key while this process runs.");
            Thread.sleep(1500);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("medium-observer-healthcare-live");
        if (!f.hasKey("active")) {
            f.set("active", "baseline");
        }
        return f;
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) return def;
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    static int readInt(Folder p, String key, int def) {
        try {
            return Integer.parseInt(read(p, key, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    private LivePostureApp() {}
}
