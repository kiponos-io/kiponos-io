---
title: "Cap Live Sports Odds and Liability Exposure in Real Time — No Java Restart (Kiponos SDK)"
published: false
tags: java, sports, betting, fintech
description: Change max decimal odds, market suspension thresholds, and per-event liability caps in Java sportsbook pricing services while matches stay in play. Kiponos local reads on every quote update.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-sports-live-odds-caps.md
main_image: https://litter.catbox.moe/00t07a.jpg
---

Champions League semifinal minute 78. A red card flips win probability and your in-play engine offers **14.5 decimal** on the underdog — three ticks above `max_odds_decimal: 12.0` from `sportsbook-limits.yml`, but the constant was never the problem. The **liability cap** on that market was already breached two goals ago, and traders still cannot tighten limits without a Java rolling restart.

The trading desk lead shouts over the floor noise:

> "**Suspend** corner markets, drop max odds to **8.0** on match winner, and cut exposure cap **50%** — now, not after deploy."

Every second of frozen caps is open liability on a live feed quoted thousands of times per minute.

**`max_odds_decimal` is not house edge architecture — it is tonight's risk appetite.**

[Kiponos.io](https://kiponos.io) lets trading ops move odds and liability policy **while the pricing engine keeps quoting** — WebSocket deltas, in-memory reads on every market update.

## The problem: static limits on the in-play pricing hot path

```java
@Service
public class LegacyOddsCapService {
    @Value("${sportsbook.max_odds_decimal:12.0}")
    private double maxOddsDecimal;

    @Value("${sportsbook.max_liability_usd:250000}")
    private int maxLiabilityUsd;

    public QuoteDecision capQuote(Market market, double rawOdds, int currentExposureUsd) {
        if (rawOdds > maxOddsDecimal) {
            return QuoteDecision.cap(maxOddsDecimal, "odds_ceiling");
        }
        if (currentExposureUsd >= maxLiabilityUsd) {
            return QuoteDecision.suspend("liability_cap");
        }
        return QuoteDecision.publish(rawOdds);
    }
}
```

Sportsbook limits usually come from:

1. **YAML at league season start** — in-play dynamics shift per minute
2. **Trader spreadsheet → batch job** — too slow for red-card moments
3. **Redis poll per quote** — adds RTT on a path that must stay sub-millisecond for config

| What teams say | What production does |
|----------------|---------------------|
| "Odds caps are risk committee policy" | Committee intent ≠ JVM updated mid-match |
| "Trading platform UI handles suspension" | Your Java pricing core still enforces floats |
| "We'll pre-build conservative profiles" | Derbies and red cards do not read profiles |
| "Liability is a ledger problem" | **Pre-ledger caps** stop bad quotes from publishing |

## The Aha: odds ceilings and liability caps are operational trading knobs

Store sportsbook ops policy under `sportsbook/limits` in Kiponos. Each `capQuote()` reads event-specific `max_odds_decimal`, `max_liability_usd`, and market suspension flags from the in-memory tree. When traders lower match-winner cap to `8.0`, the **next** quote update sees it — no pod restart.

Same pattern as [live fraud thresholds](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-fraud-payment-routing.md) — operational floats on the money path, local reads, async deltas.

## What is Kiponos.io — for sportsbook pricing

Kiponos connects your Spring Boot in-play pricing service to a live config tree. Profile `['sportsbook']['prod']['odds']` hydrates at startup. Dashboard edits are **WebSocket deltas**. `kiponos.path("sportsbook", "limits", eventId).getFloat("max_odds_decimal")` is a **local read** — no remote call on every price tick during live play.

## Architecture

![Architecture diagram](https://files.catbox.moe/rxzvgu.png)

## Example config tree

```yaml
sportsbook/
  limits/
    default/
      max_odds_decimal: 12.0
      max_liability_usd: 250000
      min_odds_decimal: 1.01
    events/
      UCL_SEMI_LEG2/
        max_odds_decimal: 12.0
        max_liability_usd: 250000
        match_winner_cap: 8.0
        corners_market_suspended: false
    markets/
      corners/
        max_liability_usd: 40000
        suspend_on_red_card: true
  global/
    halt_in_play_pricing: false
    liability_haircut_percent: 100
    auto_suspend_on_goal: false
  risk/
    surge_mode: false
    max_odds_decimal_floor: 3.0
```

## Bootstrap and integration (Spring Boot 3)

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
@Service
public class KiponosOddsCapService {

    private final Kiponos kiponos;

    public KiponosOddsCapService(Kiponos kiponos) {
        this.kiponos = kiponos;
        kiponos.afterValueChanged(change -> {
            if (change.path().startsWith("sportsbook/")) {
                log.warn("Sportsbook policy changed: {} → {}", change.path(), change.newValue());
            }
        });
    }

    public QuoteDecision capQuote(Market market, double rawOdds, int currentExposureUsd) {
        if (kiponos.path("sportsbook", "global").getBool("halt_in_play_pricing", false)) {
            return QuoteDecision.suspend("global_halt");
        }

        String eventId = market.eventId();
        var eventLimits = kiponos.path("sportsbook", "limits", "events", eventId);
        if (!eventLimits.exists()) {
            eventLimits = kiponos.path("sportsbook", "limits", "default");
        }

        var marketLimits = kiponos.path("sportsbook", "limits", "markets", market.type());
        if (marketLimits.getBool("suspend_on_red_card", false) && market.lastEventWasRedCard()) {
            return QuoteDecision.suspend("red_card_policy");
        }
        if (eventLimits.getBool("corners_market_suspended", false) && "corners".equals(market.type())) {
            return QuoteDecision.suspend("market_suspended");
        }

        double maxOdds = eventLimits.getFloat("max_odds_decimal", 12.0);
        if ("match_winner".equals(market.type())) {
            maxOdds = Math.min(maxOdds, eventLimits.getFloat("match_winner_cap", maxOdds));
        }
        if (kiponos.path("sportsbook", "risk").getBool("surge_mode", false)) {
            maxOdds = Math.min(maxOdds, kiponos.path("sportsbook", "risk").getFloat("max_odds_decimal_floor", 3.0));
        }

        int maxLiability = marketLimits.getInt("max_liability_usd",
                eventLimits.getInt("max_liability_usd", 250_000));
        int haircut = kiponos.path("sportsbook", "global").getInt("liability_haircut_percent", 100);
        int effectiveCap = maxLiability * haircut / 100;

        if (currentExposureUsd >= effectiveCap) {
            return QuoteDecision.suspend("liability_cap");
        }
        if (rawOdds > maxOdds) {
            return QuoteDecision.cap(maxOdds, "odds_ceiling");
        }
        return QuoteDecision.publish(rawOdds);
    }
}
```

Every `getFloat()` / `getInt()` is a **local memory read** — safe on pricing paths that emit thousands of quote updates per minute.

## Real scenarios

| Event | Frozen YAML reflex | Kiponos path |
|-------|-------------------|--------------|
| Red card chaos | Emergency deploy | `sportsbook/limits/markets/corners/suspend_on_red_card` + live suspend |
| Goal flurry liability spike | Open exposure until restart | `sportsbook/global/liability_haircut_percent: 50` |
| Derby day risk appetite | Pre-season profile wrong | `sportsbook/risk/surge_mode: true` |
| Trader typo recovery | Revert PR under pressure | Restore `match_winner_cap` from dashboard audit |
| Feed integrity doubt | Global kill switch | `sportsbook/global/halt_in_play_pricing: true` |

## Performance — why in-play pricing stays fast

- One WebSocket per pricing JVM — not one config fetch per quote tick
- `getFloat("max_odds_decimal")` is O(1) on the cached tree — noise next to feed parser I/O
- Delta updates — liability haircut change sends one patch, not full limits file
- Pricing threads never block on trading desk database polls
- `afterValueChanged` audit trail when risk moves caps during live play — regulatory friendly

## Compare to alternatives

| Approach | Tighten odds cap during red card | Per-quote read cost | Event-specific limits |
|----------|----------------------------------|---------------------|------------------------|
| `sportsbook-limits.yml` | PR + deploy | Zero (frozen) | Code branches |
| Trading UI only | Disconnected from Java core | N/A | UI–engine drift |
| Poll risk Redis | Possible | Cache RTT × quote volume | Key sprawl |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** | **Folder per event** |

## When not to use Kiponos for live odds

| Case | Better approach |
|------|-----------------|
| Core pricing model / implied probability math | Quant research pipeline |
| Regulatory license jurisdiction list | Compliance baseline in Git |
| Payment settlement rails | Payments platform |
| Replacing feed provider | Vendor integration project |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['sportsbook']['prod']['odds']`.
2. Add `io.kiponos:sdk-boot-3` to your in-play pricing service.
3. Create `sportsbook/limits/default` with `max_odds_decimal`, `max_liability_usd`.
4. Replace `@Value` limit reads with `kiponos.path(...)`.
5. Game day: simulate quote burst in staging, lower `match_winner_cap` live, re-cap same market — published odds change **without pod restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Java sportsbook. Cap liability while the match stays live.*