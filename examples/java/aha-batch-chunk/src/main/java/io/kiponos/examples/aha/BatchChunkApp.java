package io.kiponos.examples.aha;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/** Aha example: Batch chunk size live
 * Tree: examples / aha-batch-chunk / chunk-size
 */
public final class BatchChunkApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("chunk-size=" + read(p, "chunk-size", "100"));
            System.out.println("Batch chunk size live");
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-batch-chunk");
        if (!f.hasKey("chunk-size")) f.set("chunk-size", "100");
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
    private BatchChunkApp() {}
}
