package io.kiponos.examples.agentic;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

/**
 * A Marketplace Skill Runs in the Agent's Laptop. I Needed a Live Trust Set
 * Product: senses · Agent host: Cursor
 * Hub leaf: examples/agentic-mcp-0913-am-skill-trust/skill-trust (default core,reviewed)
 * Pain: Marketplace skill ran in the agent laptop with no isolation
 *
 * Four SDK peers share this leaf: Java, Python, React-Node, Angular-Node.
 * Never put Connect tokens in a SPA.
 */
public final class AgenticMcp0913AmSkillTrustApp {
    public static final String KEY = "skill-trust";
    public static final String DEFAULT = "core,reviewed";
    public static final String FOLDER = "agentic-mcp-0913-am-skill-trust";

    public record Decision(String value, String action, boolean proceed) {}

    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            String v = args.length > 0 ? args[0] : read(p, KEY, DEFAULT);
            Decision d = decide(v);
            System.out.println("examples/" + FOLDER + "/" + KEY + "=" + d.value());
            System.out.println("action=" + d.action() + " proceed=" + d.proceed());
            // next MCP / tool call sees dashboard edits — no host restart
            Thread.sleep(400L);
        } finally {
            k.disconnect();
        }
    }

    public static Decision decide(String raw) {
        String v = norm(raw);
        boolean proceed = !v.isBlank();
        return new Decision(v, proceed ? "honor_trusted_skills" : "skill_not_trusted", proceed);
    }

    static String norm(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        return raw.trim();
    }

    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder()
                .folderOrCreate("examples")
                .folderOrCreate(FOLDER);
        if (!f.hasKey(KEY)) {
            f.set(KEY, DEFAULT);
        }
        return f;
    }

    static String read(Folder p, String key, String def) {
        if (!p.hasKey(key)) {
            return def;
        }
        String r = p.get(key);
        return r == null || r.isBlank() ? def : r.trim();
    }

    private AgenticMcp0913AmSkillTrustApp() {}
}
