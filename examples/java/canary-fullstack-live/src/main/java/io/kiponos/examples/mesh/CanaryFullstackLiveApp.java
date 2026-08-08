    package io.kiponos.examples.mesh;

    import io.kiponos.sdk.Kiponos;
    import io.kiponos.sdk.configs.Folder;

    /**
     * Canary Percent That Moves BFF, Workers, and UI Mirrors Together
     * Hub leaf: release/canary-percent (default 5)
     * Peers: Java + Python + @kiponos/react + @kiponos/angular — same Team profile tree
     */
    public final class CanaryFullstackLiveApp {
        public static final String KEY = "canary-percent";
        public static final String DEFAULT = "5";
        public static final String PATH_LABEL = "release/canary-percent";

        public static void main(String[] args) throws Exception {
            Kiponos k = Kiponos.createForCurrentTeam();
            try {
                Folder p = ensure(k);
                int v = readInt(p, KEY, Integer.parseInt(DEFAULT));
        System.out.println(KEY + "=" + v);
                // hot path: every peer honors the same Team leaf without restart
                Thread.sleep(1200L);
            } finally {
                k.disconnect();
            }
        }

        static Folder ensure(Kiponos k) {
            Folder f = k.getRootFolder()
            .folderOrCreate("release");
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

static int readInt(Folder p, String key, int def) {
    try {
        return Integer.parseInt(read(p, key, String.valueOf(def)));
    } catch (Exception e) {
        return def;
    }
}

        private CanaryFullstackLiveApp() {}
    }
