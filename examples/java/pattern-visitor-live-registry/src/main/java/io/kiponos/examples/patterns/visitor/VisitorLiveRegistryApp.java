package io.kiponos.examples.patterns.visitor;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Visitor — which visitors run is live CSV.
 * Tree: patterns/visitor/export/visitors = json,csv,metrics
 */
public final class VisitorLiveRegistryApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(visit(p, "order-1"));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("visitor").folderOrCreate("export");
        if (!f.hasKey("visitors")) f.set("visitors", "json,csv");
        return f;
    }
    static List<String> visit(Folder policy, String node) {
        List<String> out = new ArrayList<>();
        for (String v : read(policy, "visitors", "json").split(",")) {
            String id = v.trim().toLowerCase();
            if (id.isEmpty()) continue;
            out.add(id + ".visit(" + node + ")");
        }
        return out;
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k); return r == null || r.isBlank() ? d : r.trim();
    }
    private VisitorLiveRegistryApp() {}
}
