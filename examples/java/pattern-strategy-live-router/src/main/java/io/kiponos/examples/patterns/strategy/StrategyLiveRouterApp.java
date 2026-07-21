package io.kiponos.examples.patterns.strategy;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

import java.util.Locale;
import java.util.Map;

/**
 * Super Pattern: Strategy — Live Strategy Router.
 *
 * Classic GoF Strategy: encapsulate algorithms behind a common interface.
 * Frozen form: pick the strategy in code or at bean construction, redeploy to swap.
 *
 * Super form: the active strategy id (and optional knobs) live in Kiponos.
 * Ops or a remote SDK can set {@code patterns/strategy/checkout/active} while
 * this process keeps authorizing — local {@code get()} on every decision.
 *
 * Tree:
 *   patterns / strategy / checkout / active          = flat | volume | loyalty
 *   patterns / strategy / checkout / volume-threshold = int (cents)
 *   patterns / strategy / checkout / loyalty-bps      = int (basis points off)
 */
public final class StrategyLiveRouterApp {

    private static final String PATTERNS = "patterns";
    private static final String STRATEGY = "strategy";
    private static final String CHECKOUT = "checkout";
    private static final String ACTIVE = "active";
    private static final String VOLUME_THRESHOLD = "volume-threshold";
    private static final String LOYALTY_BPS = "loyalty-bps";

    private static final Map<String, PricingStrategy> STRATEGIES = Map.of(
            "flat", new FlatPricingStrategy(),
            "volume", new VolumePricingStrategy(),
            "loyalty", new LoyaltyPricingStrategy()
    );

    public static void main(String[] args) throws InterruptedException {
        long cartCents = args.length > 0 ? Long.parseLong(args[0]) : 12_500L;
        boolean loyaltyMember = args.length > 1 && isTruthy(args[1]);

        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensureStrategyFolder(kiponos);
            Quote quote = priceCart(policy, cartCents, loyaltyMember);

            System.out.println("========================================");
            System.out.println("  Kiponos Super Pattern: Live Strategy");
            System.out.println("  path: patterns / strategy / checkout");
            System.out.println("  active: " + quote.strategyId());
            System.out.println("  cart:   " + cartCents + " cents");
            System.out.println("  total:  " + quote.totalCents() + " cents");
            System.out.println("  note:   " + quote.detail());
            System.out.println("========================================");
            System.out.println();
            System.out.println("Change \"active\" to flat | volume | loyalty in the hub.");
            System.out.println("Next priceCart() uses the new algorithm — no redeploy.");

            Thread.sleep(2_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureStrategyFolder(Kiponos kiponos) {
        Folder root = kiponos.getRootFolder();
        Folder patterns = root.folderOrCreate(PATTERNS);
        Folder strategy = patterns.folderOrCreate(STRATEGY);
        Folder checkout = strategy.folderOrCreate(CHECKOUT);
        if (!checkout.hasKey(ACTIVE)) {
            checkout.set(ACTIVE, "flat");
            System.out.println("Created default active=flat");
        }
        if (!checkout.hasKey(VOLUME_THRESHOLD)) {
            checkout.set(VOLUME_THRESHOLD, "10000");
        }
        if (!checkout.hasKey(LOYALTY_BPS)) {
            checkout.set(LOYALTY_BPS, "150");
        }
        return checkout;
    }

    /**
     * Hot-path pricing: resolve strategy from local hub cache, then run pure algorithm.
     */
    static Quote priceCart(Folder policy, long cartCents, boolean loyaltyMember) {
        String id = readActive(policy);
        PricingStrategy strategy = STRATEGIES.getOrDefault(id, STRATEGIES.get("flat"));
        StrategyContext ctx = new StrategyContext(
                cartCents,
                loyaltyMember,
                readInt(policy, VOLUME_THRESHOLD, 10_000),
                readInt(policy, LOYALTY_BPS, 150)
        );
        long total = strategy.priceCents(ctx);
        return new Quote(id, cartCents, total, strategy.describe(ctx));
    }

    static String readActive(Folder policy) {
        if (!policy.hasKey(ACTIVE)) {
            return "flat";
        }
        String raw = policy.get(ACTIVE);
        if (raw == null || raw.isBlank()) {
            return "flat";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    static int readInt(Folder policy, String key, int defaultValue) {
        if (!policy.hasKey(key)) {
            return defaultValue;
        }
        String raw = policy.get(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    static boolean isTruthy(String raw) {
        if (raw == null) {
            return false;
        }
        return "yes".equalsIgnoreCase(raw)
                || "true".equalsIgnoreCase(raw)
                || "on".equalsIgnoreCase(raw)
                || "1".equals(raw);
    }

    /** Shared context passed into each strategy (params may come from hub). */
    record StrategyContext(
            long cartCents,
            boolean loyaltyMember,
            int volumeThresholdCents,
            int loyaltyBps
    ) {
    }

    record Quote(String strategyId, long cartCents, long totalCents, String detail) {
    }

    interface PricingStrategy {
        long priceCents(StrategyContext ctx);

        String describe(StrategyContext ctx);
    }

    static final class FlatPricingStrategy implements PricingStrategy {
        @Override
        public long priceCents(StrategyContext ctx) {
            return ctx.cartCents();
        }

        @Override
        public String describe(StrategyContext ctx) {
            return "flat: no discount";
        }
    }

    static final class VolumePricingStrategy implements PricingStrategy {
        @Override
        public long priceCents(StrategyContext ctx) {
            if (ctx.cartCents() >= ctx.volumeThresholdCents()) {
                // 5% off over threshold
                return Math.round(ctx.cartCents() * 0.95);
            }
            return ctx.cartCents();
        }

        @Override
        public String describe(StrategyContext ctx) {
            return "volume: 5% off when cart >= " + ctx.volumeThresholdCents() + " cents";
        }
    }

    static final class LoyaltyPricingStrategy implements PricingStrategy {
        @Override
        public long priceCents(StrategyContext ctx) {
            if (!ctx.loyaltyMember()) {
                return ctx.cartCents();
            }
            long bps = Math.max(0, Math.min(ctx.loyaltyBps(), 9_000));
            return Math.round(ctx.cartCents() * (10_000 - bps) / 10_000.0);
        }

        @Override
        public String describe(StrategyContext ctx) {
            return "loyalty: " + ctx.loyaltyBps() + " bps off for members";
        }
    }

    private StrategyLiveRouterApp() {
    }
}
