---
title: "accessTokenValiditySeconds=3600 Was Security Policy — We Shortened It Live During the Token Leak Scare (Spring Security OAuth)"
published: false
tags: java, oauth, springsecurity, devops
description: OAuth access token TTL feels like security policy frozen in authorization server config. When leak response demands shorter-lived tokens, validity seconds are operational — Kiponos feeds live token policy without auth server restart.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-oauth-token-ttl.md
main_image: https://raw.githubusercontent.com/kiponos-io/kiponos-io/master/docs/devto-cover-aha-oauth-token-ttl.jpg
---

Security bridge call minute 12. Threat intel confirms **active abuse** of long-lived mobile tokens issued before the patch. Your authorization server still mints access tokens with **3600 seconds** validity because `accessTokenValiditySeconds=3600` lives in `AuthorizationServerSettings` configuration blessed in last year's security baseline audit.

Incident commander wants tokens capped at **900 seconds** until credential rotation completes. The security architect says:

> "Token TTL is **security policy**. We do not change validity without change advisory board and client impact analysis."

But stolen tokens are exercising APIs **now**. Access token TTL is not a annual policy PDF — it is **how long a compromised bearer remains useful tonight**.

**The Aha:** read `access_token_validity_sec` from [Kiponos.io](https://kiponos.io) when issuing tokens — ops sets `900` live while the authorization server keeps running.

## The problem: token TTL frozen at authorization server boot

```java
@Configuration
public class AuthorizationServerConfig {

    private static final int ACCESS_TOKEN_VALIDITY_SEC = 3600;
    private static final int REFRESH_TOKEN_VALIDITY_SEC = 86400;

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("https://auth.example.com")
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            context.getClaims().claim("ttl_policy", ACCESS_TOKEN_VALIDITY_SEC);
            // TokenGenerator uses fixed validity from bean wiring
        };
    }
}
```

Or YAML in legacy Spring Security OAuth:

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        token:
          access-token-time-to-live: 3600s
```

Changing TTL historically meant **auth server restart** — invalidating sessions mid-incident and delaying containment. Problems:

1. **Long-lived stolen tokens** — every minute at 3600s is attack surface
2. **Deploy to shorten** — while abuse continues
3. **Cannot tighten temporarily** — without permanent YAML churn

| What teams say | What production does |
|----------------|---------------------|
| "3600s was CAB-approved security policy" | Incidents need **temporary** tightening |
| "Clients cache tokens for full hour" | Shorter TTL forces refresh — that is the point during leaks |
| "Refresh token policy is separate" | Access TTL is the first containment lever |
| "OAuth settings belong in security baseline Git" | Validity seconds are operational risk response |

## What is Kiponos.io — for OAuth token policy

[Kiponos.io](https://kiponos.io) stores operational auth knobs under profile `['auth']['prod']['oauth']`. WebSocket deltas patch the in-memory tree. `getInt("access_token_validity_sec")` is a **local read** in your `OAuth2TokenCustomizer` — no config server RTT per token issuance.

Git keeps **issuer URL, signing keys, and client registrations**; the hub keeps **token seconds this incident**.

## Architecture

![Architecture diagram](https://files.catbox.moe/79zxj4.png)

## Config tree

```yaml
oauth/
  tokens/
    mobile:
      access_token_validity_sec: 3600
      refresh_token_validity_sec: 86400
      enabled: true
    partner_m2m:
      access_token_validity_sec: 1800
      enabled: true
  ops/
    leak_response_mode: false
    leak_access_token_validity_sec: 900
    leak_refresh_validity_sec: 3600
  audit/
    log_ttl_on_issue: true
    min_validity_floor_sec: 300
```

## Integration (Spring Authorization Server)

```java
@Configuration
public class KiponosConfig {

    @Bean
    public Kiponos kiponos(
            @Value("${kiponos.team-id}") String teamId,
            @Value("${kiponos.access-key}") String accessKey,
            @Value("${kiponos.profile-path}") String profilePath) {
        return Kiponos.builder()
                .teamId(teamId)
                .accessKey(accessKey)
                .profilePath(profilePath)
                .build();
    }
}
```

```java
@Component
public class LiveTokenValidityCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final Kiponos kiponos;

    public LiveTokenValidityCustomizer(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("oauth/tokens")
                    || change.path().startsWith("oauth/ops")) {
                log.warn("OAuth TTL policy: {} → {}", change.path(), change.newValue());
            }
        });
    }

    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) return;

        var cfg = kiponos.path("oauth", "tokens", "mobile");
        if (!cfg.getBool("enabled", true)) {
            throw new OAuth2AuthenticationException("mobile tokens disabled");
        }

        int validitySec = resolveAccessValiditySec();
        int floor = kiponos.path("oauth", "audit").getInt("min_validity_floor_sec", 300);
        validitySec = Math.max(validitySec, floor);

        Instant issuedAt = Instant.now();
        context.getClaims()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(validitySec));

        if (kiponos.path("oauth", "audit").getBool("log_ttl_on_issue", true)) {
            log.info("issued access token ttl={}s client={}", validitySec,
                    context.getRegisteredClient().getClientId());
        }
    }

    private int resolveAccessValiditySec() {
        if (kiponos.path("oauth", "ops").getBool("leak_response_mode", false)) {
            return kiponos.path("oauth", "ops").getInt("leak_access_token_validity_sec", 900);
        }
        return kiponos.path("oauth", "tokens", "mobile").getInt("access_token_validity_sec", 3600);
    }
}
```

Token leak scare? Ops enables `leak_response_mode` and `leak_access_token_validity_sec: 900`. **Next issued tokens** expire in fifteen minutes — existing JVM keeps running.

## Real scenarios

| Event | `ACCESS_TOKEN_VALIDITY_SEC = 3600` policy | Kiponos path |
|-------|-------------------------------------------|--------------|
| Active token abuse detected | Stolen tokens valid 60 min | `leak_response_mode: true` live |
| Rotation complete | Still 900s until security deploys | Disable leak mode from dashboard |
| Mobile app release week | Three YAML branches for TTL experiments | Hub profile `mobile/strict` |
| Compliance audit | CAB minutes from 2024 | Dashboard audit on `oauth/ops` |

Pair with [live session timeout tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-aha-session-timeout.md) for browser sessions in the same incident.

## Performance — why token issuance stays fast

- **`getInt()` once per token issued** — not per API call downstream
- **One WebSocket** per authorization server JVM
- **Shorter TTL increases refresh traffic** — operational tradeoff, not Kiponos overhead
- **Delta updates** — leak mode toggles two keys instantly
- **Customizer runs at issuance** — local read is nanoseconds vs JWT signing

## Compare to alternatives

| Approach | Shorten TTL during leak scare | Per-token overhead |
|----------|------------------------------|-------------------|
| Hard-coded `3600` | Redeploy auth server | Zero (frozen) |
| `@RefreshScope` auth beans | Context recycle | Session disruption |
| Revoke all tokens via DB script | Blast radius | Ops-heavy |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for OAuth TTL

| Case | Better approach |
|------|-----------------|
| Signing key rotation and JWKS URI | PKI + GitOps |
| OAuth client_id registration | Security-reviewed migrations |
| Switching to opaque tokens + introspection | Architecture change |
| TTL of 30s breaking all mobile clients without coordination | Client impact analysis first |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['auth']['prod']['oauth']`.
2. Add `io.kiponos:sdk-boot-3` to your authorization server.
3. Create `oauth/tokens/mobile` with validity and leak response keys.
4. Wire `LiveTokenValidityCustomizer` with `resolveAccessValiditySec()`.
5. Staging: enable `leak_response_mode`, mint token, confirm `exp` claim reflects 900s **without server restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — OAuth token TTL is live risk response, not security policy frozen in the baseline audit.*