---
title: "Retune ABR Bitrate Ladder Caps During a Live Stream CDN Storm — No Python Worker Restart (Kiponos SDK)"
published: false
tags: python, media, streaming, cdn
description: Change max bitrate rungs, congestion backoff, and device-tier ladders in Python transcoding workers while viewers watch live. Kiponos local reads on every manifest generation.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-media-bitrate-ladder.md
main_image: https://files.catbox.moe/i8rg0m.jpg
---

Championship overtime minute 3. Concurrent viewers cross **2.1 million** on your live HLS feed. CDN rebuffer rates spike in the US-East PoP. Your manifest generator still caps the top rung at `max_bitrate_mbps: 8.0` — copied into `ladder.py` when your average live event drew 400k viewers.

The stream ops lead types in the war room:

> "Drop the top two rungs and extend the **4 Mbps** tier — we cannot wait for a deploy while the CDN catches fire."

The transcoding fleet is healthy. The ladder constants are not. Every minute of frozen policy pushes more viewers into stall-and-retry loops.

**`max_bitrate_mbps` is not architecture. It is how hard you push the CDN tonight.**

[Kiponos.io](https://kiponos.io) feeds Python manifest and ladder workers live parameters — WebSocket deltas, in-memory reads on every playlist refresh.

## The problem: module constants on the manifest hot path

```python
# ladder.py — unchanged since last season
MAX_BITRATE_MBPS = 8.0
MIN_BITRATE_MBPS = 0.5
CONGESTION_BACKOFF = 0.75
TOP_RUNGS = 6

def build_ladder(source_profile: VideoProfile) -> list[Rung]:
    rungs = generate_rungs(source_profile, MIN_BITRATE_MBPS, MAX_BITRATE_MBPS)
    return rungs[:TOP_RUNGS]
```

Problems:

1. **Redeploy transcoding workers** — while viewers buffer during peak
2. **Per-event YAML branches** — ops cannot predict which PoP fails
3. **Poll Redis for ladder** — adds RTT inside manifest refresh loops

| What teams say | What production does |
|----------------|---------------------|
| "ABR ladder is a transcoding design choice" | Congestion response is **minute-by-minute** ops |
| "CDN will auto-scale" | Origin and edge policies still need rung caps |
| "We'll pre-build conservative and aggressive manifests" | Live sports do not follow your branch calendar |
| "Players adapt automatically" | Bad ladders waste bandwidth before player heuristics recover |

## The Aha: ladder rungs are operational capacity

Store ladder policy under `transcode/abr` in Kiponos. Each `build_ladder()` reads `max_bitrate_mbps`, `top_rungs`, and `congestion_backoff` from the in-memory tree. When ops lowers the cap to `5.5`, the **next** manifest generation reflects it — no worker restart.

## What is Kiponos.io — for Python streaming workers

[Kiponos.io](https://kiponos.io) is a config hub with Java and Python SDKs. `Kiponos.create_for_current_team()` connects over WebSocket, hydrates the tree for a profile like `['media']['prod']['transcode']`, and serves **local** `get_float()` / `get_int()` on the hot path.

Updates are **async deltas** — changing `max_bitrate_mbps` patches one key in memory. Your manifest loop never blocks on the network waiting for config.

`after_value_changed` logs ladder flips or triggers manifest cache invalidation when `force_manifest_refresh` toggles true.

## Architecture

![Architecture diagram](https://litter.catbox.moe/mncpo4.png)

## Example config tree

```yaml
transcode/
  abr/
    max_bitrate_mbps: 8.0
    min_bitrate_mbps: 0.5
    top_rungs: 6
    congestion_backoff: 0.75
    force_manifest_refresh: false
  tiers/
    mobile/
      max_bitrate_mbps: 4.5
      top_rungs: 4
    tv/
      max_bitrate_mbps: 12.0
      top_rungs: 8
  live/
    sports_mode: true
    emergency_cap_mbps: 5.5
    stall_threshold_percent: 3.0
```

## Python integration (manifest worker)

```python
import logging
from kiponos import Kiponos

log = logging.getLogger(__name__)

kiponos = Kiponos.create_for_current_team()
# Profile: ['media']['prod']['transcode'] via KIPONOS_PROFILE env


def _abr_cfg(device_tier: str = "default"):
    if device_tier in ("mobile", "tv"):
        return kiponos.path("transcode", "tiers", device_tier)
    return kiponos.path("transcode", "abr")


def effective_max_mbps(device_tier: str = "default") -> float:
    live = kiponos.path("transcode", "live")
    cfg = _abr_cfg(device_tier)
    if live.get_bool("sports_mode", False):
        emergency = live.get_float("emergency_cap_mbps", 0.0)
        if emergency > 0:
            return min(cfg.get_float("max_bitrate_mbps", 8.0), emergency)
    return cfg.get_float("max_bitrate_mbps", 8.0)


def build_ladder(source_profile: VideoProfile, device_tier: str = "default") -> list[Rung]:
    cfg = _abr_cfg(device_tier)
    min_mbps = cfg.get_float("min_bitrate_mbps", 0.5)
    max_mbps = effective_max_mbps(device_tier)
    top = cfg.get_int("top_rungs", 6)
    backoff = cfg.get_float("congestion_backoff", 0.75)

    rungs = generate_rungs(source_profile, min_mbps, max_mbps * backoff)
    return rungs[:top]


kiponos.after_value_changed(
    lambda change: log.info("ABR policy changed: %s → %s", change.path, change.new_value)
    if change.path.startswith("transcode/")
    else None
)
```

Every `get_float()` is a **local memory read** — safe inside manifest refresh loops that run every few seconds during live events.

## Real scenarios

| Event | `MAX_BITRATE_MBPS = 8.0` folklore | Kiponos path |
|-------|-----------------------------------|--------------|
| CDN congestion spike | Redeploy ladder workers | `transcode/live/emergency_cap_mbps: 5.5` |
| Mobile-heavy audience | Separate deploy target | `transcode/tiers/mobile/max_bitrate_mbps` live |
| Post-game bandwidth relief | Someone forgets to restore cap | Ops raises `emergency_cap_mbps` back to `0` (disabled) |
| Rebuffer SLA breach | Manual player team intervention | Lower `top_rungs` and raise `congestion_backoff` |
| Failover to backup origin | Branch per origin in Git | Toggle `force_manifest_refresh` for instant playlist bump |

## Performance — why manifest generation stays fast

- One WebSocket per worker process — not one config fetch per playlist segment
- `get_float("max_bitrate_mbps")` is O(1) on the cached tree — noise next to transcoding CPU
- Delta updates — emergency cap change sends one key, not a full ladder redeploy
- No process restart — Celery/asyncio workers keep serving live refresh cadence
- `after_value_changed` runs on the WebSocket thread; invalidate manifest cache there, not per segment

## Compare to alternatives

| Approach | Lower top rung during CDN storm | Per-manifest read cost |
|----------|--------------------------------|------------------------|
| Module constant in `ladder.py` | Redeploy workers | Zero (frozen) |
| `os.environ` ladder vars | Rolling restart | Zero after restart |
| Poll Redis / Consul | Possible | Network RTT × refresh rate |
| Feature flag "conservative mode" | Boolean only | Still need numeric caps |
| **Kiponos SDK** | **Dashboard (seconds)** | **Memory read** |

## When not to use Kiponos for ABR ladders

| Case | Better approach |
|------|-----------------|
| Codec migration (H.264 → AV1) | Transcoder image + pipeline change |
| DRM license server URLs | Secrets manager |
| Replacing ladder with per-title ML | Offline model training |
| Setting max bitrate above encoder hardware limit | Capacity planning first |

## Getting started (15 minutes)

1. [TeamPro at kiponos.io](https://kiponos.io) — profile `['media']['prod']['transcode']`.
2. `pip install kiponos` — set `KIPONOS_ID`, `KIPONOS_ACCESS`, `KIPONOS_PROFILE`.
3. Create `transcode/abr` with `max_bitrate_mbps`, `top_rungs`, `congestion_backoff`.
4. Replace module-level `MAX_BITRATE_MBPS` with `effective_max_mbps()`.
5. Game day: simulate CDN stall metrics in staging, set `emergency_cap_mbps` live, regenerate manifest — top rung drops **without worker restart**.

**Further reading:**

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

---

*Kiponos.io — real-time config for Python streaming. Softer ladders while the game stays live.*