package io.kiponos.examples.patterns.builder;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/** Super Pattern: Builder — live defaults + validation severity.
 * Tree: patterns/builder/report/page-size, include-charts, severity=warn|error
 */
public final class BuilderLiveDefaultsApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            Report r = build(p, args.length > 0 ? args[0] : "Q1");
            System.out.println(r);
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("builder").folderOrCreate("report");
        if (!f.hasKey("page-size")) f.set("page-size", "50");
        if (!f.hasKey("include-charts")) f.set("include-charts", "yes");
        if (!f.hasKey("severity")) f.set("severity", "warn");
        return f;
    }
    static Report build(Folder policy, String title) {
        int page = readInt(policy, "page-size", 50);
        boolean charts = truthy(read(policy, "include-charts", "yes"));
        String severity = read(policy, "severity", "warn");
        if (page < 1) {
            if ("error".equalsIgnoreCase(severity)) throw new IllegalArgumentException("page-size");
            page = 50;
        }
        return new Report(title, page, charts, severity);
    }
    static boolean truthy(String s) {
        return s != null && (s.equalsIgnoreCase("yes")||s.equalsIgnoreCase("true")||s.equals("1"));
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k); return r == null || r.isBlank() ? d : r.trim();
    }
    static int readInt(Folder p, String k, int d) {
        try { return Integer.parseInt(read(p, k, String.valueOf(d))); } catch (Exception e) { return d; }
    }
    record Report(String title, int pageSize, boolean charts, String severity) {}
    private BuilderLiveDefaultsApp() {}
}
