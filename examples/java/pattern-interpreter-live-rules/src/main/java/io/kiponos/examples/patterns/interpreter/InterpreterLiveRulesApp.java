package io.kiponos.examples.patterns.interpreter;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Interpreter — mini rules language from hub (fail-closed).
 * Tree: patterns/interpreter/access/rules = role=admin => allow; country=US => allow; * => deny
 */
public final class InterpreterLiveRulesApp {
    public static void main(String[] args) throws Exception {
        String role = args.length > 0 ? args[0] : "admin";
        String country = args.length > 1 ? args[1] : "US";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(eval(p, Map.of("role", role, "country", country)));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("interpreter").folderOrCreate("access");
        if (!f.hasKey("rules")) f.set("rules", "role=admin => allow; country=US => allow; * => deny");
        return f;
    }
    static String eval(Folder policy, Map<String, String> ctx) {
        String rules = read(policy, "rules", "* => deny");
        for (String rule : rules.split(";")) {
            String r = rule.trim();
            if (r.isEmpty()) continue;
            String[] parts = r.split("=>");
            if (parts.length != 2) continue;
            String cond = parts[0].trim();
            String action = parts[1].trim().toLowerCase(Locale.ROOT);
            if ("*".equals(cond) || matches(cond, ctx)) {
                return action + " (" + cond + ")";
            }
        }
        return "deny (fail-closed)";
    }
    static boolean matches(String cond, Map<String, String> ctx) {
        String[] kv = cond.split("=");
        if (kv.length != 2) return false;
        String v = ctx.getOrDefault(kv[0].trim().toLowerCase(Locale.ROOT), "");
        return v.equalsIgnoreCase(kv[1].trim());
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k);
        return r == null || r.isBlank() ? d : r.trim();
    }
    private InterpreterLiveRulesApp() {}
}
