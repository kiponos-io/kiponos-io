package io.kiponos.examples.patterns.iterator;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Iterator — page size live.
 * Tree: patterns/iterator/catalog/page-size = int
 */
public final class IteratorLivePageSizeApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            int page = readInt(p, "page-size", 25);
            List<String> items = List.of("a","b","c","d","e","f","g");
            System.out.println("page0=" + pageOf(items, page, 0));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("iterator").folderOrCreate("catalog");
        if (!f.hasKey("page-size")) f.set("page-size", "25");
        return f;
    }
    static List<String> pageOf(List<String> items, int pageSize, int pageIndex) {
        int from = Math.max(0, pageIndex * pageSize);
        if (from >= items.size()) return List.of();
        int to = Math.min(items.size(), from + pageSize);
        return items.subList(from, to);
    }
    static int readInt(Folder p, String k, int d) {
        try {
            if (!p.hasKey(k)) return d;
            return Integer.parseInt(p.get(k).trim());
        } catch (Exception e) { return d; }
    }
    private IteratorLivePageSizeApp() {}
}
