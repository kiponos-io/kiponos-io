package io.kiponos.examples.patterns.prototype;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Prototype — which template id to clone is live.
 * Tree: patterns/prototype/docs/active-template = invoice-v1|invoice-v2
 */
public final class PrototypeLiveRegistryApp {
    private static final Map<String, String> REGISTRY = Map.of(
            "invoice-v1", "INVOICE template v1 body",
            "invoice-v2", "INVOICE template v2 body");
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(cloneTemplate(p));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("prototype").folderOrCreate("docs");
        if (!f.hasKey("active-template")) f.set("active-template", "invoice-v1");
        return f;
    }
    static String cloneTemplate(Folder policy) {
        String id = read(policy, "active-template", "invoice-v1");
        return REGISTRY.getOrDefault(id, REGISTRY.get("invoice-v1")) + " [clone of " + id + "]";
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k); return r == null || r.isBlank() ? d : r.trim();
    }
    private PrototypeLiveRegistryApp() {}
}
