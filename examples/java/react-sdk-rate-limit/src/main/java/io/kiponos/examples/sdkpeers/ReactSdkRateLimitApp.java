        package io.kiponos.examples.sdkpeers;

        import io.kiponos.sdk.Kiponos;
        import io.kiponos.sdk.configs.Folder;

        /**
         * Rate Limits the BFF Honors Without Redeploying Node
         * Hub: limits/rps-cap (default 40)
         * Pain: RPS caps baked into Node process env and only change on restart
         * Peers: Java + Python + @kiponos/react (Node server)
         */
        public final class ReactSdkRateLimitApp {
            public static final String FOLDER = "limits";
            public static final String KEY = "rps-cap";
            public static final String DEFAULT = "40";

            public static void main(String[] args) throws Exception {
                Kiponos k = Kiponos.createForCurrentTeam();
                try {
                    Folder p = ensure(k);
                    int v = readInt(p, KEY, Integer.parseInt(DEFAULT));
        System.out.println(KEY + "=" + v);
        // hot path: honor live live BFF RPS cap without restart
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

            private ReactSdkRateLimitApp() {}
        }
