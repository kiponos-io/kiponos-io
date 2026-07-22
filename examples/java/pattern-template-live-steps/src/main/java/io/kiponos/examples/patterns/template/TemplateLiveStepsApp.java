package io.kiponos.examples.patterns.template;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;
import java.util.*;

/** Super Pattern: Template Method — optional steps toggled live.
 * Tree: patterns/template/onboard/steps = validate,enrich,persist,notify (csv)
 */
public final class TemplateLiveStepsApp {
    public static void main(String[] args) throws Exception {
        Kiponos k = Kiponos.createForCurrentTeam();
        try {
            Folder p = ensure(k);
            System.out.println("steps=" + runTemplate(p, "user-1"));
            Thread.sleep(1500);
        } finally { k.disconnect(); }
    }
    static Folder ensure(Kiponos k) {
        Folder f = k.getRootFolder().folderOrCreate("patterns").folderOrCreate("template").folderOrCreate("onboard");
        if (!f.hasKey("steps")) f.set("steps", "validate,enrich,persist,notify");
        return f;
    }
    static List<String> runTemplate(Folder policy, String id) {
        List<String> steps = new ArrayList<>();
        for (String s : read(policy, "steps", "validate,enrich,persist,notify").split(",")) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty()) continue;
            steps.add(t + "(" + id + ")");
        }
        return steps;
    }
    static String read(Folder p, String k, String d) {
        if (!p.hasKey(k)) return d;
        String r = p.get(k);
        return r == null || r.isBlank() ? d : r.trim();
    }
    private TemplateLiveStepsApp() {}
}
