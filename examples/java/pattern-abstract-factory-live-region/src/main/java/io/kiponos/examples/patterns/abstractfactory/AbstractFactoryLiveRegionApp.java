package io.kiponos.examples.patterns.abstractfactory;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.Locale;

/**
 * Super Pattern: Abstract Factory — live region family (EU vs US UI+payments).
 * Tree: patterns/abstract-factory/region/family = us|eu
 */
public final class AbstractFactoryLiveRegionApp {
    public static void main(String[] args) throws InterruptedException {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensure(k);
            RegionFactory f = createFamily(policy);
            System.out.println("========================================");
            System.out.println("  Super Pattern: Live Abstract Factory");
            System.out.println("  family:   " + f.family());
            System.out.println("  currency: " + f.currency().code());
            System.out.println("  tax:      " + f.tax().label());
            System.out.println("========================================");
            Thread.sleep(2000L);
        } finally { k.disconnect(); }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("abstract-factory").folderOrCreate("region");
        if (!f.hasKey("family")) f.set("family", "us");
        return f;
    }

    static RegionFactory createFamily(Folder policy) {
        String fam = read(policy, "family", "us").toLowerCase(Locale.ROOT);
        return "eu".equals(fam) ? new EuFactory() : new UsFactory();
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) return def;
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    interface Currency { String code(); }
    interface TaxRule { String label(); }
    interface RegionFactory {
        String family();
        Currency currency();
        TaxRule tax();
    }
    static final class UsFactory implements RegionFactory {
        public String family() { return "us"; }
        public Currency currency() { return () -> "USD"; }
        public TaxRule tax() { return () -> "US-sales-tax"; }
    }
    static final class EuFactory implements RegionFactory {
        public String family() { return "eu"; }
        public Currency currency() { return () -> "EUR"; }
        public TaxRule tax() { return () -> "EU-VAT"; }
    }
    private AbstractFactoryLiveRegionApp() {}
}
