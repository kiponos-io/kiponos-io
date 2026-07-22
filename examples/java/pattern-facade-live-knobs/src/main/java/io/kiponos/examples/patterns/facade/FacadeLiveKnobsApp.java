package io.kiponos.examples.patterns.facade;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * Super Pattern: Facade — stable checkout() API; guts (tax, inventory, notify) knobs live.
 * Tree: patterns/facade/checkout/tax-bps, inventory-check, notify = yes|no
 */
public final class FacadeLiveKnobsApp {
    public static void main(String[] args) throws InterruptedException {
        long cents = args.length > 0 ? Long.parseLong(args[0]) : 10_000L;
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensure(k);
            CheckoutResult r = checkout(policy, cents);
            System.out.println("========================================");
            System.out.println("  Super Pattern: Live Facade");
            System.out.println("  total: " + r.totalCents());
            System.out.println("  steps: " + r.steps());
            System.out.println("========================================");
            Thread.sleep(2000L);
        } finally { k.disconnect(); }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("facade").folderOrCreate("checkout");
        if (!f.hasKey("tax-bps")) f.set("tax-bps", "800");
        if (!f.hasKey("inventory-check")) f.set("inventory-check", "yes");
        if (!f.hasKey("notify")) f.set("notify", "yes");
        return f;
    }

    static CheckoutResult checkout(Folder policy, long cartCents) {
        StringBuilder steps = new StringBuilder("price");
        long total = cartCents;
        int taxBps = readInt(policy, "tax-bps", 800);
        total += Math.round(cartCents * taxBps / 10_000.0);
        steps.append("+tax(").append(taxBps).append("bps)");
        if (truthy(read(policy, "inventory-check", "yes"))) {
            steps.append("+inventory");
        }
        if (truthy(read(policy, "notify", "yes"))) {
            steps.append("+notify");
        }
        return new CheckoutResult(total, steps.toString());
    }

    static boolean truthy(String s) {
        return s != null && (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true") || s.equals("1"));
    }
    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) return def;
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }
    static int readInt(Folder p, String key, int def) {
        try { return Integer.parseInt(read(p, key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
    record CheckoutResult(long totalCents, String steps) {}
    private FacadeLiveKnobsApp() {}
}
