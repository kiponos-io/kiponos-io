package io.kiponos.examples.springrest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST surface that demonstrates live hub reads — not {@code @Value}-frozen fields.
 *
 * <ul>
 *   <li>{@code GET /api/ops} — current timeout + greeting from Kiponos</li>
 *   <li>{@code GET /api/hello} — greeting only (hot-path style)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class OpsController {

    private final LiveOpsConfig liveOps;

    public OpsController(LiveOpsConfig liveOps) {
        this.liveOps = liveOps;
    }

    @GetMapping("/ops")
    public Map<String, Object> ops() {
        liveOps.ensureOpsFolder();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("folder", "examples / spring-boot-rest");
        body.put("requestTimeoutMs", liveOps.requestTimeoutMs());
        body.put("greeting", liveOps.greeting());
        body.put("note", "Flip keys in the Kiponos dashboard — no restart. @Value would still be frozen.");
        return body;
    }

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", liveOps.greeting());
        body.put("requestTimeoutMs", liveOps.requestTimeoutMs());
        return body;
    }
}
