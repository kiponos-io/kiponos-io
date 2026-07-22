package io.kiponos.examples.patterns.command;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.Locale;

/** Super Pattern: Command — live enable/dry-run per command id.
 * Tree: patterns/command/dispatch/refund.enabled = yes|no
 *       patterns/command/dispatch/refund.dry-run = yes|no
 */
public final class CommandLiveDispatchApp {
    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "refund";
        long amount = args.length > 1 ? Long.parseLong(args[1]) : 500L;
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println(dispatch(p, cmd, amount));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("command").folderOrCreate("dispatch");
        if (!f.hasKey("refund.enabled")) f.set("refund.enabled", "yes");
        if (!f.hasKey("refund.dry-run")) f.set("refund.dry-run", "no");
        return f;
    }
    static String dispatch(Folder policy, String cmd, long amount) {
        String c = cmd.toLowerCase(Locale.ROOT);
        if (!truthy(read(policy, c + ".enabled", "yes"))) return "blocked: disabled " + c;
        boolean dry = truthy(read(policy, c + ".dry-run", "no"));
        return (dry ? "DRY-RUN " : "EXEC ") + c + " amount=" + amount;
    }
    static boolean truthy(String s) {
        return s != null && (s.equalsIgnoreCase("yes")||s.equalsIgnoreCase("true")||s.equals("1"));
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k);
        return r == null || r.isBlank() ? d : r.trim();
    }
    private CommandLiveDispatchApp() {}
}
