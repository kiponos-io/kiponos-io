package io.kiponos.examples.patterns.composite;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Composite — per-node enable/weight live.
 * Tree: patterns/composite/score/nodes = base:1,risk:2,loyalty:1 (id:weight)
 *       patterns/composite/score/enabled = base,risk,loyalty
 */
public final class CompositeLiveRulesApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("score=" + score(p, Map.of("base", 10, "risk", 3, "loyalty", 5)));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("composite").folderOrCreate("score");
        if (!f.hasKey("nodes")) f.set("nodes", "base:1,risk:2,loyalty:1");
        if (!f.hasKey("enabled")) f.set("enabled", "base,risk,loyalty");
        return f;
    }
    static double score(Folder policy, Map<String, Integer> values) {
        Set<String> enabled = new HashSet<>();
        for (String e : read(policy, "enabled", "").split(",")) {
            if (!e.trim().isEmpty()) enabled.add(e.trim().toLowerCase());
        }
        double sum = 0, wsum = 0;
        for (String part : read(policy, "nodes", "").split(",")) {
            String[] kv = part.trim().split(":");
            if (kv.length != 2) continue;
            String id = kv[0].trim().toLowerCase();
            if (!enabled.contains(id)) continue;
            double w = Double.parseDouble(kv[1].trim());
            int v = values.getOrDefault(id, 0);
            sum += v * w; wsum += w;
        }
        return wsum == 0 ? 0 : sum / wsum;
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k); return r == null || r.isBlank() ? d : r.trim();
    }
    private CompositeLiveRulesApp() {}
}
