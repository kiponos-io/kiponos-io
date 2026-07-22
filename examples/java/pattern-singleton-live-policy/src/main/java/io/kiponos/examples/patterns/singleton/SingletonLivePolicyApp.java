package io.kiponos.examples.patterns.singleton;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/** Super Pattern: Singleton — one instance, live *policy* knobs.
 * Tree: patterns/singleton/gate/mode = open|half|closed
 */
public final class SingletonLivePolicyApp {
    private static final SingletonLivePolicyApp INSTANCE = new SingletonLivePolicyApp();
    private SingletonLivePolicyApp() {}
    public static SingletonLivePolicyApp get() { return INSTANCE; }

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("mode=" + get().mode(p));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("singleton").folderOrCreate("gate");
        if (!f.hasKey("mode")) f.set("mode", "open");
        return f;
    }
    String mode(Folder policy) {
        if (!policy.hasKey("mode")) return "open";
        String r = policy.get("mode");
        return r == null || r.isBlank() ? "open" : r.trim().toLowerCase();
    }
}
