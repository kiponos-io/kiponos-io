package io.kiponos.examples.sdkpeers;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * A/B Weights You Can Rebalance Mid-Experiment
 * Hub: experiments/ab-weights (default 50,50)
 * Pain: experiment weights frozen in client bundles until the next SPA ship
 * Peers: Java + Python + @kiponos/react (Node server)
 */
public final class ReactSdkAbWeightsApp {
    public static final String FOLDER = "experiments";
    public static final String KEY = "ab-weights";
    public static final String DEFAULT = "50,50";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
System.out.println(KEY + "=" + v);
// hot path: honor live live A/B weight vector without restart
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

    private ReactSdkAbWeightsApp() {}
}
