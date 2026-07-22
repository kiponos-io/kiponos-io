package io.kiponos.examples.patterns.adapter;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.Locale;

/**
 * Super Pattern: Adapter — Live PSP (payment service provider) selection.
 * Tree: patterns/adapter/checkout/provider = stripe|adyen|braintree
 *       patterns/adapter/checkout/currency = ISO code
 */
public final class AdapterLivePspApp {
    public static void main(String[] args) throws InterruptedException {
        long cents = args.length > 0 ? Long.parseLong(args[0]) : 4999L;
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensure(k);
            ChargeResult r = charge(policy, cents);
            System.out.println("========================================");
            System.out.println("  Super Pattern: Live Adapter (PSP)");
            System.out.println("  provider: " + r.provider());
            System.out.println("  receipt:  " + r.receipt());
            System.out.println("========================================");
            Thread.sleep(2000L);
        } finally { k.disconnect(); }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("adapter").folderOrCreate("checkout");
        if (!f.hasKey("provider")) f.set("provider", "stripe");
        if (!f.hasKey("currency")) f.set("currency", "USD");
        return f;
    }

    static ChargeResult charge(Folder policy, long cents) {
        String p = read(policy, "provider", "stripe").toLowerCase(Locale.ROOT);
        String cur = read(policy, "currency", "USD").toUpperCase(Locale.ROOT);
        PaymentPort port = switch (p) {
            case "adyen" -> new AdyenAdapter();
            case "braintree" -> new BraintreeAdapter();
            default -> new StripeAdapter();
        };
        return new ChargeResult(port.name(), port.charge(cents, cur));
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) return def;
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    interface PaymentPort { String name(); String charge(long cents, String currency); }
    static final class StripeAdapter implements PaymentPort {
        public String name() { return "stripe"; }
        public String charge(long c, String cur) { return "stripe_ch_" + c + "_" + cur; }
    }
    static final class AdyenAdapter implements PaymentPort {
        public String name() { return "adyen"; }
        public String charge(long c, String cur) { return "adyen_psp_" + c + "_" + cur; }
    }
    static final class BraintreeAdapter implements PaymentPort {
        public String name() { return "braintree"; }
        public String charge(long c, String cur) { return "bt_tx_" + c + "_" + cur; }
    }
    record ChargeResult(String provider, String receipt) {}
    private AdapterLivePspApp() {}
}
