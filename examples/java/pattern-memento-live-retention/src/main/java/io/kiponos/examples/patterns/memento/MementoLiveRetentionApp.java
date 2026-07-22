package io.kiponos.examples.patterns.memento;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Memento — retention depth live.
 * Tree: patterns/memento/editor/max-snapshots = int
 */
public final class MementoLiveRetentionApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Deque<String> snaps = new ArrayDeque<>();
            int max = readInt(p, "max-snapshots", 3);
            for (String s : List.of("v1", "v2", "v3", "v4")) push(snaps, max, s);
            System.out.println("snapshots=" + snaps + " max=" + max);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("memento").folderOrCreate("editor");
        if (!f.hasKey("max-snapshots")) f.set("max-snapshots", "3");
        return f;
    }
    static void push(Deque<String> snaps, int max, String s) {
        snaps.addLast(s);
        while (snaps.size() > max) snaps.removeFirst();
    }
    static int readInt(Folder p, String k, int d) {
        try {
            if (!p.hasKey(k)) return d;
            return Integer.parseInt(p.get(k).trim());
        } catch (Exception e) { return d; }
    }
    private MementoLiveRetentionApp() {}
}
