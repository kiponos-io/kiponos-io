        package io.kiponos.examples.sdkpeers;

        import io.kiponos.sdk.Kiponos;
        import io.kiponos.sdk.configs.Folder;

        /**
         * Live Rate Limits the Angular Admin Proxy Honors
         * Hub: limits/rps-cap (default 25)
         * Pain: admin proxy RPS only changes with pod restart
         * Peers: Java + Python + @kiponos/angular (Node server)
         */
        public final class AngularSdkRateLimitApp {
            public static final String FOLDER = "limits";
            public static final String KEY = "rps-cap";
            public static final String DEFAULT = "25";

            public static void main(String[] args) throws Exception {
                Kiponos k = Kiponos.createForCurrentTeam();
                try {
                    Folder p = ensure(k);
                    int v = readInt(p, KEY, Integer.parseInt(DEFAULT));
        System.out.println(KEY + "=" + v);
        // hot path: honor live Angular admin proxy RPS cap without restart
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

            private AngularSdkRateLimitApp() {}
        }
