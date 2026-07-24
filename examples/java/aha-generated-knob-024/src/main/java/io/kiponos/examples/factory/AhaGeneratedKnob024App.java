package io.kiponos.examples.factory;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Generated golden example for content factory.
 * Generic live knob knob-24 for continuous stream demo 24
 * Hub: examples/aha-generated-knob-024/knob-24 (default 34)
 */
public final class AhaGeneratedKnob024App {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("knob-24=" + read(p, "knob-24", "34"));
            System.out.println("Generic live knob knob-24 for continuous stream demo 24");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-generated-knob-024");
        if (!f.hasKey("knob-24")) {
            f.set("knob-24", "34");
        }
        return f;
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) {
            return def;
        }
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    static int readInt(Folder p, String key, int def) {
        try {
            return Integer.parseInt(read(p, key, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    private AhaGeneratedKnob024App() {}
}
