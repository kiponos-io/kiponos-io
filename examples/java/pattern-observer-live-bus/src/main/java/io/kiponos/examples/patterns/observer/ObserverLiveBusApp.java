package io.kiponos.examples.patterns.observer;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/**
 * Super Pattern: Observer — live enable + debounce for event bus subscribers.
 * Tree: patterns/observer/bus/enabled-subscribers = metrics,audit,webhook (csv)
 *       patterns/observer/bus/debounce-ms = int
 */
public final class ObserverLiveBusApp {
    public static void main(String[] args) throws Exception {
        String event = args.length > 0 ? args[0] : "order.paid";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            List<String> trail = publish(p, event);
            System.out.println("event=" + event + " trail=" + trail);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("observer").folderOrCreate("bus");
        if (!f.hasKey("enabled-subscribers")) f.set("enabled-subscribers", "metrics,audit");
        if (!f.hasKey("debounce-ms")) f.set("debounce-ms", "100");
        return f;
    }
    static List<String> publish(Folder policy, String event) {
        List<String> enabled = csv(read(policy, "enabled-subscribers", "metrics,audit"));
        int debounce = readInt(policy, "debounce-ms", 100);
        List<String> trail = new ArrayList<>();
        trail.add("debounce=" + debounce + "ms");
        for (String s : enabled) {
            trail.add(s + ":onEvent(" + event + ")");
        }
        return trail;
    }
    static List<String> csv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String p : raw.split(",")) {
            String t = p.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k);
        return r == null || r.isBlank() ? d : r.trim();
    }
    static int readInt(Folder p, String k, int d) {
        try { return Integer.parseInt(read(p, k, String.valueOf(d))); } catch (Exception e) { return d; }
    }
    private ObserverLiveBusApp() {}
}
