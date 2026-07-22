package io.kiponos.examples.patterns.factory;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

import java.util.Locale;
import java.util.Map;

/**
 * Super Pattern: Factory Method — Live Notification Channel Factory.
 *
 * Classic: factory switch in code. Super: product id live in Kiponos.
 *
 * Tree:
 *   patterns / factory / notify / product     = email | sms | push | slack
 *   patterns / factory / notify / from-email  = string
 *   patterns / factory / notify / slack-hook  = string (demo label)
 */
public final class FactoryLiveChannelApp {

    private static final String PRODUCT = "product";
    private static final String FROM = "from-email";
    private static final String SLACK = "slack-hook";

    public static void main(String[] args) throws InterruptedException {
        String message = args.length > 0 ? args[0] : "Order 42 shipped";

        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensureFactoryFolder(kiponos);
            Notifier n = createNotifier(policy);
            String receipt = n.send(message);

            System.out.println("========================================");
            System.out.println("  Kiponos Super Pattern: Live Factory");
            System.out.println("  path: patterns / factory / notify");
            System.out.println("  product: " + n.channel());
            System.out.println("  receipt: " + receipt);
            System.out.println("========================================");
            System.out.println("Set product=email|sms|push|slack in the hub — next send uses it.");
            Thread.sleep(2_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureFactoryFolder(Kiponos kiponos) {
        Folder notify = kiponos.getRootFolder()
                .folderOrCreate("patterns")
                .folderOrCreate("factory")
                .folderOrCreate("notify");
        if (!notify.hasKey(PRODUCT)) {
            notify.set(PRODUCT, "email");
            System.out.println("Created default product=email");
        }
        if (!notify.hasKey(FROM)) {
            notify.set(FROM, "noreply@example.com");
        }
        if (!notify.hasKey(SLACK)) {
            notify.set(SLACK, "#ops-alerts");
        }
        return notify;
    }

    static Notifier createNotifier(Folder policy) {
        String id = read(policy, PRODUCT, "email").toLowerCase(Locale.ROOT);
        String from = read(policy, FROM, "noreply@example.com");
        String slack = read(policy, SLACK, "#ops-alerts");
        return switch (id) {
            case "sms" -> new SmsNotifier();
            case "push" -> new PushNotifier();
            case "slack" -> new SlackNotifier(slack);
            default -> new EmailNotifier(from);
        };
    }

    static String read(Folder policy, String key, String def) {
        if (!policy.hasKey(key)) {
            return def;
        }
        String raw = policy.get(key);
        return raw == null || raw.isBlank() ? def : raw.trim();
    }

    interface Notifier {
        String channel();

        String send(String message);
    }

    static final class EmailNotifier implements Notifier {
        private final String from;

        EmailNotifier(String from) {
            this.from = from;
        }

        @Override
        public String channel() {
            return "email";
        }

        @Override
        public String send(String message) {
            return "email from=" + from + " body=" + message;
        }
    }

    static final class SmsNotifier implements Notifier {
        @Override
        public String channel() {
            return "sms";
        }

        @Override
        public String send(String message) {
            return "sms body=" + message;
        }
    }

    static final class PushNotifier implements Notifier {
        @Override
        public String channel() {
            return "push";
        }

        @Override
        public String send(String message) {
            return "push body=" + message;
        }
    }

    static final class SlackNotifier implements Notifier {
        private final String channelName;

        SlackNotifier(String channelName) {
            this.channelName = channelName;
        }

        @Override
        public String channel() {
            return "slack";
        }

        @Override
        public String send(String message) {
            return "slack " + channelName + " body=" + message;
        }
    }

    private FactoryLiveChannelApp() {
    }
}
