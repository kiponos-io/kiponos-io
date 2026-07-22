package io.kiponos.examples.patterns.chain;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Super Pattern: Chain of Responsibility — Live Fraud Handler Chain.
 *
 * Classic: handlers ordered in code. Super: order + knobs live in Kiponos.
 *
 * Tree:
 *   patterns / chain / fraud / order              = amount-cap,geo,velocity (csv)
 *   patterns / chain / fraud / amount-cap-cents    = int
 *   patterns / chain / fraud / blocked-countries   = csv ISO codes
 *   patterns / chain / fraud / velocity-max        = int (orders in window)
 */
public final class ChainLiveFraudApp {

    private static final String ORDER = "order";
    private static final String AMOUNT_CAP = "amount-cap-cents";
    private static final String BLOCKED = "blocked-countries";
    private static final String VELOCITY = "velocity-max";

    public static void main(String[] args) throws InterruptedException {
        long amount = args.length > 0 ? Long.parseLong(args[0]) : 25_000L;
        String country = args.length > 1 ? args[1] : "US";
        int recentOrders = args.length > 2 ? Integer.parseInt(args[2]) : 2;

        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensureFraudFolder(kiponos);
            Decision d = evaluate(policy, new Payment(amount, country, recentOrders));

            System.out.println("========================================");
            System.out.println("  Kiponos Super Pattern: Live Chain");
            System.out.println("  path: patterns / chain / fraud");
            System.out.println("  allowed: " + d.allowed());
            System.out.println("  reason:  " + d.reason());
            System.out.println("  trail:   " + String.join(" → ", d.trail()));
            System.out.println("========================================");
            System.out.println("Edit handler order/knobs in the hub — next payment uses them.");
            Thread.sleep(2_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureFraudFolder(Kiponos kiponos) {
        Folder fraud = kiponos.getRootFolder()
                .folderOrCreate("patterns")
                .folderOrCreate("chain")
                .folderOrCreate("fraud");
        if (!fraud.hasKey(ORDER)) {
            fraud.set(ORDER, "amount-cap,geo,velocity");
            System.out.println("Created default order=amount-cap,geo,velocity");
        }
        if (!fraud.hasKey(AMOUNT_CAP)) {
            fraud.set(AMOUNT_CAP, "100000");
        }
        if (!fraud.hasKey(BLOCKED)) {
            fraud.set(BLOCKED, "KP,IR,SY");
        }
        if (!fraud.hasKey(VELOCITY)) {
            fraud.set(VELOCITY, "5");
        }
        return fraud;
    }

    static Decision evaluate(Folder policy, Payment payment) {
        List<String> order = csv(read(policy, ORDER, "amount-cap,geo,velocity"));
        long cap = readLong(policy, AMOUNT_CAP, 100_000L);
        Set<String> blocked = csv(read(policy, BLOCKED, "KP,IR,SY")).stream()
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        int velocityMax = (int) readLong(policy, VELOCITY, 5L);

        Map<String, FraudHandler> handlers = new LinkedHashMap<>();
        handlers.put("amount-cap", p -> p.amountCents() > cap
                ? Decision.reject("amount-cap", "amount " + p.amountCents() + " > cap " + cap)
                : Decision.pass("amount-cap"));
        handlers.put("geo", p -> blocked.contains(p.country().toUpperCase(Locale.ROOT))
                ? Decision.reject("geo", "blocked country " + p.country())
                : Decision.pass("geo"));
        handlers.put("velocity", p -> p.recentOrders() > velocityMax
                ? Decision.reject("velocity", "recentOrders " + p.recentOrders() + " > " + velocityMax)
                : Decision.pass("velocity"));

        List<String> trail = new ArrayList<>();
        for (String id : order) {
            FraudHandler h = handlers.get(id);
            if (h == null) {
                trail.add(id + ":skip-unknown");
                continue;
            }
            Decision step = h.check(payment);
            trail.add(id + ":" + (step.allowed() ? "ok" : "reject"));
            if (!step.allowed()) {
                return new Decision(false, step.reason(), List.copyOf(trail));
            }
        }
        return new Decision(true, "all handlers passed", List.copyOf(trail));
    }

    static String read(Folder policy, String key, String def) {
        if (!policy.hasKey(key)) {
            return def;
        }
        String raw = policy.get(key);
        return raw == null || raw.isBlank() ? def : raw.trim();
    }

    static long readLong(Folder policy, String key, long def) {
        try {
            return Long.parseLong(read(policy, key, String.valueOf(def)));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    static List<String> csv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    record Payment(long amountCents, String country, int recentOrders) {
    }

    record Decision(boolean allowed, String reason, List<String> trail) {
        static Decision pass(String handler) {
            return new Decision(true, handler + " ok", List.of());
        }

        static Decision reject(String handler, String reason) {
            return new Decision(false, reason, List.of());
        }
    }

    @FunctionalInterface
    interface FraudHandler {
        Decision check(Payment payment);
    }

    private ChainLiveFraudApp() {
    }
}
