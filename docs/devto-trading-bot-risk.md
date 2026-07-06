---
title: "Retune Trading Bot Risk Caps in Real Time (Kiponos Python SDK)"
published: true
tags: python, fintech, trading, realtime
description: Change position sizes, stop-loss, and max drawdown while your Python trading bot runs. Kiponos pushes risk deltas over WebSocket; the bot reads locally with zero latency.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-trading-bot-risk.md
main_image: https://files.catbox.moe/6orbpk.jpg
---

Markets move faster than deploy pipelines. Your bot is live, volatility spikes, and you need to **cut position size**, **tighten stop-loss**, or **halt new entries** — now, not after the next release.

[Kiponos.io](https://kiponos.io) gives Python trading systems **live risk parameters**: in-memory reads in the execution loop, WebSocket deltas from ops or a risk supervisor.

## The execution-loop rule

```python
def should_enter(kiponos, signal):
    risk = kiponos.path("risk", "intraday")
    if risk.get_bool("halt_new_entries"):
        return False
    if portfolio.drawdown() > risk.get_float("max_drawdown_pct"):
        return False
    return signal.strength > risk.get_float("min_signal_strength")
```

Every `get_*` is a **local read** — no HTTP between your bot and the exchange path.

## Risk config tree

```yaml
risk/
  intraday/
    max_position_usd: 25000
    max_drawdown_pct: 3.5
    stop_loss_pct: 1.2
    halt_new_entries: false
    min_signal_strength: 0.65
  symbols/
    BTC/
      max_position_usd: 10000
    ETH/
      max_position_usd: 8000
```

## Supervisor + operator control

| Actor | Action |
|-------|--------|
| Risk analyst | Lower `max_position_usd` in dashboard during news event |
| Supervisor algo | Set `halt_new_entries: true` when VaR exceeds limit |
| Trader | Per-symbol caps without restarting bot process |

Same pattern as [live ML hyperparameter tuning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-realtime-ml-training.md): writers update Kiponos, readers stay in the hot loop.

## Performance

- WebSocket background thread applies deltas
- Bot thread only touches cached values
- Changing one float does not reload full config tree

Start at [kiponos.io](https://kiponos.io). Resources: [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Python. Adjust risk while the market is open.*