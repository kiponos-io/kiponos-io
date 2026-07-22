package io.kiponos.examples.aha;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/** Aha example: Kafka max poll records live
 * Tree: examples / aha-kafka-max-poll / max-poll-records
 */
public final class KafkaMaxPollApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("max-poll-records=" + read(p, "max-poll-records", "50"));
            System.out.println("Kafka max poll records live");
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-kafka-max-poll");
        if (!f.hasKey("max-poll-records")) f.set("max-poll-records", "50");
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
    private KafkaMaxPollApp() {}
}
