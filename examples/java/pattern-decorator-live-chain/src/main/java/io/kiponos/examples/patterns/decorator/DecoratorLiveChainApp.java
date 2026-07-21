package io.kiponos.examples.patterns.decorator;

import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Super Pattern: Decorator — Live Decorator Chain.
 *
 * Classic GoF Decorator: wrap a core component with cross-cutting layers.
 * Frozen form: wrapper order is wired at compile/DI time.
 *
 * Super form: which layers are on (and their knobs) live in Kiponos.
 * Ops or a remote SDK can enable metrics, retry, or cache without redeploy.
 *
 * Tree:
 *   patterns / decorator / http-client / chain     = metrics,retry,cache (csv)
 *   patterns / decorator / http-client / retry-max  = int
 *   patterns / decorator / http-client / cache-ttl-s = int
 */
public final class DecoratorLiveChainApp {

    private static final String PATTERNS = "patterns";
    private static final String DECORATOR = "decorator";
    private static final String HTTP = "http-client";
    private static final String CHAIN = "chain";
    private static final String RETRY_MAX = "retry-max";
    private static final String CACHE_TTL = "cache-ttl-s";

    public static void main(String[] args) throws InterruptedException {
        String path = args.length > 0 ? args[0] : "/api/catalog";

        Kiponos kiponos = Kiponos.createForCurrentTeam();
        try {
            Folder policy = ensureDecoratorFolder(kiponos);
            CallResult result = execute(policy, path);

            System.out.println("========================================");
            System.out.println("  Kiponos Super Pattern: Live Decorator");
            System.out.println("  path: patterns / decorator / http-client");
            System.out.println("  chain: " + result.chainApplied());
            System.out.println("  call:  " + path);
            System.out.println("  body:  " + result.body());
            System.out.println("  log:   " + String.join(" | ", result.trace()));
            System.out.println("========================================");
            System.out.println();
            System.out.println("Edit \"chain\" (csv: metrics,retry,cache) in the hub.");
            System.out.println("Next execute() rebuilds the wrapper stack — no redeploy.");

            Thread.sleep(2_000L);
        } finally {
            kiponos.disconnect();
            System.out.println("Disconnected from Kiponos.");
        }
    }

    static Folder ensureDecoratorFolder(Kiponos kiponos) {
        Folder root = kiponos.getRootFolder();
        Folder patterns = root.folderOrCreate(PATTERNS);
        Folder decorator = patterns.folderOrCreate(DECORATOR);
        Folder http = decorator.folderOrCreate(HTTP);
        if (!http.hasKey(CHAIN)) {
            http.set(CHAIN, "metrics,retry");
            System.out.println("Created default chain=metrics,retry");
        }
        if (!http.hasKey(RETRY_MAX)) {
            http.set(RETRY_MAX, "2");
        }
        if (!http.hasKey(CACHE_TTL)) {
            http.set(CACHE_TTL, "30");
        }
        return http;
    }

    /**
     * Build decorator stack from live policy, then invoke core HTTP-ish call.
     */
    static CallResult execute(Folder policy, String path) {
        List<String> chain = parseChain(read(policy, CHAIN, "metrics,retry"));
        int retryMax = readInt(policy, RETRY_MAX, 2);
        int cacheTtl = readInt(policy, CACHE_TTL, 30);

        List<String> trace = new ArrayList<>();
        UnaryOperator<Request> core = req -> {
            trace.add("core:" + req.path());
            return new Request(req.path(), "OK body for " + req.path());
        };

        UnaryOperator<Request> pipeline = core;
        // Apply outermost last so listed order is outer→inner left-to-right
        for (int i = chain.size() - 1; i >= 0; i--) {
            String layer = chain.get(i);
            pipeline = wrap(layer, pipeline, trace, retryMax, cacheTtl);
        }

        Request out = pipeline.apply(new Request(path, null));
        return new CallResult(String.join(",", chain), out.body() != null ? out.body() : "", List.copyOf(trace));
    }

    static UnaryOperator<Request> wrap(
            String layer,
            UnaryOperator<Request> inner,
            List<String> trace,
            int retryMax,
            int cacheTtl
    ) {
        return switch (layer) {
            case "metrics" -> req -> {
                long t0 = System.nanoTime();
                Request r = inner.apply(req);
                long ms = (System.nanoTime() - t0) / 1_000_000L;
                trace.add("metrics:elapsedMs=" + ms);
                return r;
            };
            case "retry" -> req -> {
                int attempts = 0;
                RuntimeException last = null;
                while (attempts <= retryMax) {
                    attempts++;
                    try {
                        Request r = inner.apply(req);
                        trace.add("retry:attempts=" + attempts + "/max=" + retryMax);
                        return r;
                    } catch (RuntimeException ex) {
                        last = ex;
                        trace.add("retry:fail#" + attempts);
                    }
                }
                throw last != null ? last : new IllegalStateException("retry exhausted");
            };
            case "cache" -> req -> {
                // Demo: pretend cache hit after first path key (no real store across runs)
                trace.add("cache:ttl=" + cacheTtl + "s miss→inner");
                return inner.apply(req);
            };
            default -> {
                trace.add("skip:unknown=" + layer);
                yield inner;
            }
        };
    }

    static List<String> parseChain(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return out;
        }
        for (String part : csv.split(",")) {
            String p = part.trim().toLowerCase(Locale.ROOT);
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    static String read(Folder policy, String key, String defaultValue) {
        if (!policy.hasKey(key)) {
            return defaultValue;
        }
        String raw = policy.get(key);
        return raw == null || raw.isBlank() ? defaultValue : raw.trim();
    }

    static int readInt(Folder policy, String key, int defaultValue) {
        String raw = read(policy, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    record Request(String path, String body) {
    }

    record CallResult(String chainApplied, String body, List<String> trace) {
    }

    private DecoratorLiveChainApp() {
    }
}
