        package io.kiponos.examples.sdkpeers;

        import io.kiponos.sdk.Kiponos;
        import io.kiponos.sdk.configs.Folder;

        /**
         * Canary Percent Shared With Angular Admin Mirrors
         * Hub: release/canary-percent (default 5)
         * Pain: admin UI shows wrong canary state because it has its own config
         * Peers: Java + Python + @kiponos/angular (Node server)
         */
        public final class AngularSdkCanaryPercentApp {
            public static final String FOLDER = "release";
            public static final String KEY = "canary-percent";
            public static final String DEFAULT = "5";

            public static void main(String[] args) throws Exception {
                Kiponos k = Kiponos.createForCurrentTeam();
                try {
                    Folder p = ensure(k);
                    int v = readInt(p, KEY, Integer.parseInt(DEFAULT));
        System.out.println(KEY + "=" + v);
        // hot path: honor live canary percent for Angular admin mirrors without restart
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

static int readInt(Folder p, String key, int def) {
    try {
        return Integer.parseInt(read(p, key, String.valueOf(def)));
    } catch (Exception e) {
        return def;
    }
}

            private AngularSdkCanaryPercentApp() {}
        }
