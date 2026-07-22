package io.kiponos.examples.patterns.mediator;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Mediator — who may talk to whom is live CSV topology.
 * Tree: patterns/mediator/chat/edges = alice>bob,bob>carol (csv)
 */
public final class MediatorLiveTopologyApp {
    public static void main(String[] args) throws Exception {
        String from = args.length > 0 ? args[0] : "alice";
        String to = args.length > 1 ? args[1] : "bob";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(route(p, from, to));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("mediator").folderOrCreate("chat");
        if (!f.hasKey("edges")) f.set("edges", "alice>bob,bob>carol,alice>carol");
        return f;
    }
    static String route(Folder policy, String from, String to) {
        String edge = from.toLowerCase(Locale.ROOT) + ">" + to.toLowerCase(Locale.ROOT);
        Set<String> edges = new HashSet<>();
        for (String part : read(policy, "edges", "").split(",")) {
            String e = part.trim().toLowerCase(Locale.ROOT).replace(" ", "");
            if (!e.isEmpty()) edges.add(e);
        }
        return edges.contains(edge) ? "deliver " + edge : "blocked " + edge;
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k);
        return r == null || r.isBlank() ? d : r.trim();
    }
    private MediatorLiveTopologyApp() {}
}
