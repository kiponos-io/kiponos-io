package io.kiponos.examples.patterns.bridge;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.Locale;

/**
 * Super Pattern: Bridge — live implementor for a notification abstraction.
 * Tree: patterns/bridge/notify/implementor = smtp|ses|sendgrid
 */
public final class BridgeLiveImplementorApp {
    public static void main(String[] args) throws InterruptedException {
        String msg = args.length > 0 ? args[0] : "bridge demo";
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensure(k);
            String out = new Notification(createImplementor(policy)).send(msg);
            System.out.println("========================================");
            System.out.println("  Super Pattern: Live Bridge");
            System.out.println("  result: " + out);
            System.out.println("========================================");
            Thread.sleep(2000L);
        } finally { k.disconnect(); }
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("bridge").folderOrCreate("notify");
        if (!f.hasKey("implementor")) f.set("implementor", "smtp");
        return f;
    }

    static MessageSender createImplementor(Folder policy) {
        String id = read(policy, "implementor", "smtp").toLowerCase(Locale.ROOT);
        return switch (id) {
            case "ses" -> body -> "ses:" + body;
            case "sendgrid" -> body -> "sendgrid:" + body;
            default -> body -> "smtp:" + body;
        };
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) return def;
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    @FunctionalInterface interface MessageSender { String deliver(String body); }
    static final class Notification {
        private final MessageSender sender;
        Notification(MessageSender sender) { this.sender = sender; }
        String send(String body) { return sender.deliver(body); }
    }
    private BridgeLiveImplementorApp() {}
}
