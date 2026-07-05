---
title: "Partner HTTP Timeouts in Kubernetes Without a Service Mesh (Kiponos Java SDK)"
published: false
tags: java, kubernetes, springboot, resilience
description: Teams without Istio or Linkerd freeze WebClient timeouts in Spring @Bean factories. Partner brownouts need live patience per dependency — Kiponos feeds timeout policy to Java pods with local reads, no VirtualService PR.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-partner-timeout-no-mesh.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-k8s-partner-timeout-no-mesh.jpg
---

Wednesday 09:22 UTC. Tax calculation partner latency climbs to **7–9 seconds** — elevated, not down. Your checkout fleet runs on **plain Kubernetes**: no Istio, no Linkerd, no ambient mesh. Outbound calls use Spring `WebClient` with `responseTimeout(Duration.ofSeconds(3))` baked into a `@Bean` at image build time.

Pods are healthy. CPU is fine. Circuit breaker opens anyway because **every** partner call hits `TimeoutException` at 3s while the partner **would** have answered at 8s. Product insists checkout is broken; platform insists mesh adoption is **Q3**.

The integration architect says:

> "Without a mesh, timeout policy is **code**. We need a release to loosen it."

Mesh or not, **timeout milliseconds are operational patience** — not semver. You do not need a VirtualService PR to survive a brownout. You need **live client policy inside the JVM** that already runs in every pod.

**The Aha:** embed [Kiponos.io](https://kiponos.io) in your checkout Deployment. Ops raises `partners/tax_api/read_timeout_ms` from the dashboard; `LiveWebClientFactory` rebuilds the client on `afterValueChanged` — **no mesh, no pod restart, no Helm values for timeouts**.

## The problem — meshless pods with frozen WebClients

```yaml
# k8s/checkout-api/deployment.yaml — no sidecar, no mesh annotations
spec:
  template:
    spec:
      containers:
        - name: checkout-api
          image: checkout-api:2.1.0
          env:
            - name: KIPONOS_ID
              valueFrom:
                secretKeyRef:
                  name: kiponos-auth
                  key: id
            - name: KIPONOS_ACCESS
              valueFrom:
                secretKeyRef:
                  name: kiponos-auth
                  key: access
```

```java
@Bean
public WebClient taxPartnerClient() {
    return WebClient.builder()
            .baseUrl("https://tax.partner.example/v2")
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create().responseTimeout(Duration.ofSeconds(3))))
            .build();
}
```

| What teams believe | What production does |
|--------------------|---------------------|
| "Mesh gives us timeout overrides" | You have **no mesh** and a live incident |
| "ConfigMap for timeout.yaml" | Still needs Reloader + rolling restart |
| "One timeout fits all partners" | Tax API at 8s, fraud API must stay at 2s |
| "We'll add Istio next quarter" | Revenue loss is **this quarter** |

## What Kiponos.io is — meshless outbound policy

[Kiponos.io](https://kiponos.io) is a real-time config hub with a **Java SDK** that caches partner client policy **in each pod**. WebSocket deltas update the tree; `getInt("read_timeout_ms")` on every outbound call is a **local memory read** — zero network RTT on the checkout hot path.

Profile:

```
['checkout']['prod']['partners']
```

This replaces what teams often expect from `VirtualService.timeout` or `DestinationRule` — but lives **inside the application process**, same pattern as [HTTP timeout tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-http-timeout.md), scoped for **Kubernetes pods without mesh**.

Git keeps **base URLs and mTLS** reviewed. The hub keeps **operational patience** per partner.

## Architecture — no sidecar, no mesh control plane

![Architecture diagram](https://litter.catbox.moe/3yuugn.png)

One container per pod ([embedded SDK vs sidecar](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-sidecar-vs-embedded-sdk.md)). No Envoy filter chain. No xDS subscription.

## Config tree

```yaml
partners/
  tax_api/
    connect_timeout_ms: 1000
    read_timeout_ms: 3000
    max_connections: 60
    rebuild_on_change: true
    brownout_mode: false
    brownout_read_timeout_ms: 12000
  fraud_api/
    connect_timeout_ms: 800
    read_timeout_ms: 2000
    max_connections: 40
    rebuild_on_change: true
  payments_api/
    connect_timeout_ms: 500
    read_timeout_ms: 5000
    max_connections: 100
    rebuild_on_change: true
  policy/
    default_connect_timeout_ms: 1000
    default_read_timeout_ms: 3000
    log_timeout_changes: true
```

## Integration — Spring Boot 3 in Kubernetes

```java
@Configuration
public class KiponosConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey) {
        return Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath("['checkout']['prod']['partners']")
                .build();
    }
}
```

```java
@Component
public class LiveWebClientFactory {

    private final Kiponos kiponos;
    private final ConcurrentHashMap<String, WebClient> clients = new ConcurrentHashMap<>();

    public LiveWebClientFactory(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(c -> {
            if (c.path().startsWith("partners/")
                    && (c.path().contains("tax_api")
                        || c.path().contains("fraud_api")
                        || c.path().contains("payments_api"))) {
                String partner = partnerKeyFromPath(c.path());
                if (kiponos.path("partners", partner).getBool("rebuild_on_change", true)) {
                    clients.remove(partner);
                }
            }
        });
    }

    public WebClient client(String partnerKey, String baseUrl) {
        return clients.computeIfAbsent(partnerKey, k -> build(partnerKey, baseUrl));
    }

    private WebClient build(String partnerKey, String baseUrl) {
        var cfg = kiponos.path("partners", partnerKey);
        int readMs = cfg.getBool("brownout_mode", false)
                ? cfg.getInt("brownout_read_timeout_ms", 12000)
                : cfg.getInt("read_timeout_ms", 3000);
        int connectMs = cfg.getInt("connect_timeout_ms", 1000);
        int maxConn = cfg.getInt("max_connections", 80);

        ConnectionProvider provider = ConnectionProvider.builder(partnerKey)
                .maxConnections(maxConn)
                .build();
        HttpClient http = HttpClient.create(provider)
                .responseTimeout(Duration.ofMillis(readMs))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }

    private static String partnerKeyFromPath(String path) {
        // partners/tax_api/read_timeout_ms -> tax_api
        String[] parts = path.split("/");
        return parts.length >= 2 ? parts[1] : "tax_api";
    }
}
```

```java
@Service
public class TaxQuoteService {

    private final LiveWebClientFactory clients;
    private final Kiponos kiponos;

    public Mono<TaxQuote> quote(TaxRequest req) {
        WebClient wc = clients.client("tax_api", "https://tax.partner.example/v2");
        int readMs = kiponos.path("partners", "tax_api").getInt("read_timeout_ms", 3000);
        return wc.post().uri("/quote").bodyValue(req)
                .retrieve()
                .bodyToMono(TaxQuote.class)
                .timeout(Duration.ofMillis(readMs));
    }
}
```

Ops enables `brownout_mode: true` on `tax_api` only — fraud and payments clients unchanged. **Same pod age** after the tweak ([no restart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-restart.md)).

## Real scenarios

| Event | No mesh, frozen 3s bean | Kiponos per-partner policy |
|-------|-------------------------|----------------------------|
| Tax partner brownout | Checkout hard fails | `brownout_mode` + 12s read |
| Partner recovers | Still waiting 12s | Disable brownout live |
| Fraud API strict SLO | Shared timeout too loose | `fraud_api/read_timeout_ms: 2000` |
| New pod from HPA | Same image, latest hub snapshot | Connect + delta sync |
| Mesh pilot later | Duplicate policy risk | Hub remains source; mesh mirrors |

## Performance

- One WebSocket per checkout pod — not per outbound HTTP call
- `getInt("read_timeout_ms")` on `quote()` is **O(1) local**
- Factory rebuild runs on **delta**, async — request threads not blocked
- Per-partner connection pools — tax brownout does not exhaust fraud pool
- HPA scale-out: new pods receive snapshot + deltas ([SDK per pod](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-sdk-per-pod.md))

## Compare to alternatives

| Approach | Meshless K8s brownout response | Per-partner isolation |
|----------|-------------------------------|------------------------|
| Istio VirtualService | Requires mesh | Yes — but Q3 |
| ConfigMap timeout YAML | Rolling restart | One file per partner |
| `@RefreshScope` WebClient | Actuator refresh | Pool cold start |
| Hard-coded per-call timeout | Fast once | Sprawl |
| **Kiponos embedded SDK** | **Dashboard seconds** | **Tree keys per partner** |

## When not to use Kiponos for partner timeouts

| Case | Better approach |
|------|-----------------|
| Partner base URL migration after review | Git + deployment |
| mTLS cert rotation | cert-manager + trust store in image |
| Retry budgets and idempotency keys | Code + hub for **retry count** only |
| Adopting mesh for L7 policy | Mesh for traffic shift; hub for app-level patience |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['checkout']['prod']['partners']`.
2. Add `KIPONOS_ID` / `KIPONOS_ACCESS` to Deployment ([minimal pod spec](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-k8s-no-configmaps.md)).
3. Externalize **one** partner's `read_timeout_ms` into `partners/tax_api/`.
4. Deploy `LiveWebClientFactory`; remove static `Duration.ofSeconds(3)` bean.
5. Game day: staging mock at 8s latency, enable `brownout_mode`, verify checkout succeeds **without** mesh or rollout.
6. Document: mesh optional; **hub required** for meshless live patience.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [WebClient timeout Aha](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-http-timeout.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — partner timeouts without mesh, without restart, without pretending Q3 is today.*