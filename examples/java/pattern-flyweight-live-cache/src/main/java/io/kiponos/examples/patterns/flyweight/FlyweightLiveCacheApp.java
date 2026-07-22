package io.kiponos.examples.patterns.flyweight;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Flyweight — cache size/eviction live.
 * Tree: patterns/flyweight/glyphs/max-entries = int
 */
public final class FlyweightLiveCacheApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Map<String, String> cache = new LinkedHashMap<>();
            int max = readInt(p, "max-entries", 3);
            for (String g : List.of("A", "B", "C", "D")) {
                put(cache, max, g, "glyph-" + g);
            }
            System.out.println("cache=" + cache.keySet() + " max=" + max);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("flyweight").folderOrCreate("glyphs");
        if (!f.hasKey("max-entries")) f.set("max-entries", "3");
        return f;
    }
    static void put(Map<String, String> cache, int max, String k, String v) {
        cache.put(k, v);
        while (cache.size() > max) {
            String first = cache.keySet().iterator().next();
            cache.remove(first);
        }
    }
    static int readInt(Folder p, String k, int d) {
        try {
            if (!p.hasKey(k)) return d;
            return Integer.parseInt(p.get(k).trim());
        } catch (Exception e) { return d; }
    }
    private FlyweightLiveCacheApp() {}
}
