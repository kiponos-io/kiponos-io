package io.kiponos.examples.patterns.state;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Super Pattern: State — Live Order State Machine.
 *
 * Classic: transitions hard-coded in switch/enum methods.
 * Super: allowed transitions live in Kiponos so ops can freeze refunds etc.
 *
 * Tree:
 *   patterns / state / order / current = draft | paid | shipped | cancelled
 *   patterns / state / order / allowed = draft>paid,paid>shipped,paid>cancelled,draft>cancelled
 */
public final class StateLiveOrderApp {

    private static final String CURRENT = "current";
    private static final String ALLOWED = "allowed";

    public static void main(String[] args) throws InterruptedException {
        String want = args.length > 0 ? args[0] : "paid";

        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensureStateFolder(kiponos);
            TransitionResult r = tryTransition(policy, want);

            System.out.println("========================================");
            System.out.println("  Kiponos Super Pattern: Live State");
            System.out.println("  path: patterns / state / order");
            System.out.println("  from: " + r.from());
            System.out.println("  to:   " + r.to());
            System.out.println("  ok:   " + r.ok());
            System.out.println("  note: " + r.detail());
            System.out.println("========================================");
            System.out.println("Edit \"allowed\" matrix in the hub to freeze transitions live.");
            Thread.sleep(2_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureStateFolder(Kiponos kiponos) {
        Folder order = kiponos.getRootFolder()
                .folderOrCreate("patterns")
                .folderOrCreate("state")
                .folderOrCreate("order");
        if (!order.hasKey(CURRENT)) {
            order.set(CURRENT, "draft");
            System.out.println("Created default current=draft");
        }
        if (!order.hasKey(ALLOWED)) {
            order.set(ALLOWED, "draft>paid,paid>shipped,paid>cancelled,draft>cancelled");
        }
        return order;
    }

    static TransitionResult tryTransition(Folder policy, String nextRaw) {
        String from = normalize(read(policy, CURRENT, "draft"));
        String to = normalize(nextRaw);
        Set<String> edges = parseAllowed(read(policy, ALLOWED,
                "draft>paid,paid>shipped,paid>cancelled,draft>cancelled"));
        String edge = from + ">" + to;
        if (from.equals(to)) {
            return new TransitionResult(from, to, true, "already in " + to);
        }
        if (!edges.contains(edge)) {
            return new TransitionResult(from, to, false, "transition not allowed: " + edge);
        }
        policy.set(CURRENT, to);
        return new TransitionResult(from, to, true, "transitioned " + edge);
    }

    static Set<String> parseAllowed(String raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String p = part.trim().toLowerCase(Locale.ROOT).replace(" ", "");
            if (p.contains(">")) {
                out.add(p);
            }
        }
        return out;
    }

    static List<String> allowedFrom(Set<String> edges, String state) {
        List<String> next = new ArrayList<>();
        String prefix = normalize(state) + ">";
        for (String e : edges) {
            if (e.startsWith(prefix)) {
                next.add(e.substring(prefix.length()));
            }
        }
        return next;
    }

    static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    static String read(Folder policy, String key, String def) {
        if (!policy.hasKey(key)) {
            return def;
        }
        String raw = policy.get(key);
        return raw == null || raw.isBlank() ? def : raw.trim();
    }

    record TransitionResult(String from, String to, boolean ok, String detail) {
    }

    private StateLiveOrderApp() {
    }
}
