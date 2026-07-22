package io.kiponos.examples.factory;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Generated golden example for content factory.
 * overbooking percent live
 * Hub: examples/travel-overbooking-limit/overbook-pct (default 5)
 */
public final class TravelOverbookingLimitApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("overbook-pct=" + read(p, "overbook-pct", "5"));
            System.out.println("overbooking percent live");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("travel-overbooking-limit");
        if (!f.hasKey("overbook-pct")) {
            f.set("overbook-pct", "5");
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

    static int readInt(Folder p, String key, int def) {
        try {
            return Integer.parseInt(read(p, key, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    private TravelOverbookingLimitApp() {}
}
