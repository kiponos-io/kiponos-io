package io.kiponos.examples.fintech;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * FinTech processor kill switch — industry-deep variant of the 3am boolean.
 *
 * Classic pain: a flaky card acquirer is timing out; compliance wants new
 * authorizations stopped in seconds, not after a release train.
 *
 * Tree (per processor / rail):
 *   examples / fintech / processors / {acquirerId} / accept-new-auth
 *
 * Ops flips the key in the Kiponos dashboard. This process reads local memory
 * on every authorization decision — no redeploy, no restart.
 */
public final class FintechProcessorKillSwitchApp {

    private static final String EXAMPLES = "examples";
    private static final String FINTECH = "fintech";
    private static final String PROCESSORS = "processors";
    private static final String DEFAULT_ACQUIRER = "acquirer-alpha";
    private static final String ACCEPT_NEW_AUTH = "accept-new-auth";
    private static final String REASON = "disable-reason";

    public static void main(String[] args) throws InterruptedException {
        String acquirer = args.length > 0 ? args[0] : DEFAULT_ACQUIRER;

        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder processor = ensureProcessorFolder(kiponos, acquirer);
            boolean accept = readAcceptNewAuth(processor);
            String reason = readReason(processor);

            System.out.println("========================================");
            System.out.println("  Kiponos example: FinTech processor kill switch");
            System.out.println("  path: examples / fintech / processors / " + acquirer);
            System.out.println("  key:  accept-new-auth = " + accept);
            if (!accept && reason != null && !reason.isBlank()) {
                System.out.println("  reason: " + reason);
            }
            System.out.println("========================================");

            // Demo authorization path
            AuthDecision d = authorizeDemo(processor, "txn-demo-1001", 42_00L);
            System.out.println("[auth] decision=" + d.status() + " detail=" + d.detail());

            if (accept) {
                System.out.println();
                System.out.println("Flip accept-new-auth to \"no\" in the Kiponos dashboard");
                System.out.println("for this acquirer — next authorize() refuses without redeploy.");
            } else {
                System.out.println();
                System.out.println("Safe posture: new auths refused for " + acquirer + ".");
                System.out.println("Settle/capture paths can keep running (ops policy).");
            }

            Thread.sleep(3_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureProcessorFolder(Kiponos kiponos, String acquirerId) {
        Folder root = kiponos.getRootFolder();
        Folder examples = root.folderOrCreate(EXAMPLES);
        Folder fintech = examples.folderOrCreate(FINTECH);
        Folder processors = fintech.folderOrCreate(PROCESSORS);
        Folder processor = processors.folderOrCreate(acquirerId);
        if (!processor.hasKey(ACCEPT_NEW_AUTH)) {
            processor.set(ACCEPT_NEW_AUTH, "yes");
            System.out.println("Created default " + ACCEPT_NEW_AUTH + "=yes for " + acquirerId);
        }
        if (!processor.hasKey(REASON)) {
            processor.set(REASON, "");
        }
        return processor;
    }

    /**
     * Hot-path style decision: local get only (SDK in-memory tree).
     */
    static AuthDecision authorizeDemo(Folder processor, String txnId, long amountCents) {
        if (!readAcceptNewAuth(processor)) {
            String reason = readReason(processor);
            String detail = (reason == null || reason.isBlank())
                    ? "processor_disabled"
                    : "processor_disabled:" + reason;
            return new AuthDecision("REFUSED", txnId, amountCents, detail);
        }
        return new AuthDecision("APPROVED_DEMO", txnId, amountCents, "acquirer_path_open");
    }

    static boolean readAcceptNewAuth(Folder processor) {
        if (!processor.hasKey(ACCEPT_NEW_AUTH)) {
            return true;
        }
        return isTruthy(processor.get(ACCEPT_NEW_AUTH));
    }

    static String readReason(Folder processor) {
        if (!processor.hasKey(REASON)) {
            return "";
        }
        String r = processor.get(REASON);
        return r == null ? "" : r;
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

    record AuthDecision(String status, String txnId, long amountCents, String detail) {
    }

    private FintechProcessorKillSwitchApp() {
    }
}
