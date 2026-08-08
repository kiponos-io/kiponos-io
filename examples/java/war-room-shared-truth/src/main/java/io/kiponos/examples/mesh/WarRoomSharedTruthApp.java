package io.kiponos.examples.mesh;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * One War-Room Headline: Humans, Agents, and Every SDK on the Same Leaf
 * Hub leaf: warroom/headline (default steady)
 * Peers: Java + Python + @kiponos/react + @kiponos/angular — same Team profile tree
 */
public final class WarRoomSharedTruthApp {
    public static final String KEY = "headline";
    public static final String DEFAULT = "steady";
    public static final String PATH_LABEL = "warroom/headline";

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = read(p, KEY, DEFAULT);
    System.out.println(KEY + "=" + v);
            // hot path: every peer honors the same Team leaf without restart
            Thread.sleep(1200L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder()
        .folderOrCreate("warroom");
        if (!f.hasKey(KEY)) {
            f.set(KEY, DEFAULT);
        }
        return f;
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) return def;
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    private WarRoomSharedTruthApp() {}
}
