package io.kiponos.examples.patterns.proxy;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.Locale;

/**
 * Super Pattern: Proxy — live allow/deny + rate limit for a sensitive resource.
 * Tree: patterns/proxy/admin-api/enabled = yes|no
 *       patterns/proxy/admin-api/rate-per-min = int
 *       patterns/proxy/admin-api/role-allow = csv
 */
public final class ProxyLiveAccessApp {
    public static void main(String[] args) throws InterruptedException {
        String role = args.length > 0 ? args[0] : "operator";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensure(k);
            AccessResult r = invoke(policy, role, "/admin/export");
            System.out.println("========================================");
            System.out.println("  Super Pattern: Live Proxy");
            System.out.println("  allowed: " + r.allowed());
            System.out.println("  detail:  " + r.detail());
            System.out.println("========================================");
            Thread.sleep(2000L);
        } finally { k.disconnect(); }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("proxy").folderOrCreate("admin-api");
        if (!f.hasKey("enabled")) f.set("enabled", "yes");
        if (!f.hasKey("rate-per-min")) f.set("rate-per-min", "30");
        if (!f.hasKey("role-allow")) f.set("role-allow", "operator,admin");
        return f;
    }

    static AccessResult invoke(Folder policy, String role, String path) {
        if (!truthy(read(policy, "enabled", "yes"))) {
            return new AccessResult(false, "proxy disabled");
        }
        String allow = read(policy, "role-allow", "operator,admin").toLowerCase(Locale.ROOT);
        boolean roleOk = false;
        for (String part : allow.split(",")) {
            if (part.trim().equalsIgnoreCase(role)) { roleOk = true; break; }
        }
        if (!roleOk) return new AccessResult(false, "role not allowed: " + role);
        int rate = readInt(policy, "rate-per-min", 30);
        // demo resource
        return new AccessResult(true, "OK " + path + " rate=" + rate + "/min as " + role);
    }

    static boolean truthy(String s) {
        return s != null && (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("on"));
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
    record AccessResult(boolean allowed, String detail) {}
    private ProxyLiveAccessApp() {}
}
