package io.kiponos.examples.factory;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Generated golden example for content factory.
 * Generic live knob knob-5 for continuous stream demo 5
 * Hub: examples/aha-generated-knob-005/knob-5 (default 15)
 */
public final class AhaGeneratedKnob005App {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("knob-5=" + read(p, "knob-5", "15"));
            System.out.println("Generic live knob knob-5 for continuous stream demo 5");
            Thread.sleep(1500L);
        } finally {
            k.disconnect();
        }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("examples").folderOrCreate("aha-generated-knob-005");
        if (!f.hasKey("knob-5")) {
            f.set("knob-5", "15");
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

    private AhaGeneratedKnob005App() {}
}
