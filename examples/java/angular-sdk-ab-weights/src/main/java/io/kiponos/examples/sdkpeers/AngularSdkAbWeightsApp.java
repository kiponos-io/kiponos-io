package io.kiponos.examples.sdkpeers;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * A/B Weights on Angular Without a Client Bundle Ship
 * Hub: experiments/ab-weights (default 70,30)
 * Pain: Angular experiment weights ship only with the SPA
 * Peers: Java + Python + @kiponos/angular (Node server)
 */
public final class AngularSdkAbWeightsApp {
    public static final String FOLDER = "experiments";
    public static final String KEY = "ab-weights";
    public static final String DEFAULT = "70,30";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
System.out.println(KEY + "=" + v);
// hot path: honor live Angular experiment weights without restart
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

    private AngularSdkAbWeightsApp() {}
}
