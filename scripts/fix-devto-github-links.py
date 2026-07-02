#!/usr/bin/env python3
"""Replace broken github blob article links with live dev.to URLs."""

from __future__ import annotations

import json
import re
import urllib.request
from pathlib import Path

DOCS = Path(__file__).resolve().parents[1] / "docs"
API_KEY = Path.home() / ".config/devto/api_key"
COVER_CACHE = Path.home() / ".config/devto/cover-urls.json"

# Repo paths that exist on GitHub — keep as-is
KEEP_GITHUB_SUFFIXES = {
    "docs/GETTING-STARTED.md",
    "docs/PUBLIC-SANDBOX.md",
}

BLOB_RE = re.compile(
    r"https://github\.com/kiponos-io/kiponos-io/blob/master/(docs/[^\s\)\]\"']+\.md)"
)
RAW_COVER_RE = re.compile(
    r"https://raw\.githubusercontent\.com/kiponos-io/kiponos-io/master/(docs/[^\s\)\]\"']+)"
)


def load_api_key() -> str:
    return API_KEY.read_text(encoding="utf-8").strip()


def fetch_url(aid: int) -> str:
    req = urllib.request.Request(
        f"https://dev.to/api/articles/{aid}",
        headers={"api-key": load_api_key(), "User-Agent": "kiponos-link-fix/1.0"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read())
    return data.get("url") or f"https://dev.to{data.get('path', '')}"


def build_mapping() -> dict[str, str]:
    mapping: dict[str, str] = {}

    manifest = json.loads(
        (Path.home() / ".config/devto/published-manifest.json").read_text(encoding="utf-8")
    )
    for md_path, info in manifest.items():
        fn = Path(md_path).name
        if info.get("url"):
            mapping[fn] = info["url"]

    for jf in ("wave-aha-article-ids.json", "wave11-aha-article-ids.json"):
        p = DOCS / jf
        if not p.exists():
            continue
        for fn, aid in json.loads(p.read_text(encoding="utf-8")).items():
            if fn not in mapping:
                mapping[fn] = fetch_url(int(aid))

    press = json.loads(
        (Path.home() / ".config/crunchbase/press-manifest.json").read_text(encoding="utf-8")
    )
    press_by_id = {
        int(v["devto_id"]): v["url"]
        for v in press.values()
        if v.get("devto_id") and v.get("url")
    }

    extra_ids = {
        "devto-mind-reader-live-ops.md": 4045312,
        "devto-getting-started-developer-guide.md": 4047302,
        "devto-springboot-beyond-refresh-scope.md": 4044940,
        "devto-arch-gitops-vs-live-config.md": 4044906,
        "devto-arch-opentelemetry-slo-thresholds.md": 4052935,
        "devto-arch-multi-region-active-active.md": 4052951,
        "devto-arch-config-schema-versioning.md": 4052960,
        "devto-arch-sidecar-vs-embedded-sdk.md": 4053762,
        "devto-arch-disaster-recovery-live-config.md": 4053786,
        "devto-arch-cost-control-runtime.md": 4053769,
    }
    for fn, aid in extra_ids.items():
        if fn not in mapping:
            mapping[fn] = press_by_id.get(aid) or fetch_url(aid)

    return mapping


def fix_file(path: Path, mapping: dict[str, str], cover_cache: dict[str, str]) -> dict[str, int]:
    text = path.read_text(encoding="utf-8")
    original = text
    stats = {"blob": 0, "canonical": 0, "raw_cover": 0}

    def blob_replacer(match: re.Match[str]) -> str:
        suffix = match.group(1)
        if suffix in KEEP_GITHUB_SUFFIXES:
            return match.group(0)
        fn = Path(suffix).name
        if fn in mapping:
            stats["blob"] += 1
            return mapping[fn]
        return match.group(0)

    text = BLOB_RE.sub(blob_replacer, text)

    fn = path.name
    if fn in mapping:
        new_canonical = mapping[fn]
        new_text, n = re.subn(
            r"^canonical_url:\s*.+$",
            f"canonical_url: {new_canonical}",
            text,
            count=1,
            flags=re.M,
        )
        if n:
            text = new_text
            stats["canonical"] = 1

    def raw_replacer(match: re.Match[str]) -> str:
        rel = match.group(1)
        local = DOCS / Path(rel).name
        key = str(local.resolve())
        if key in cover_cache:
            stats["raw_cover"] += 1
            return cover_cache[key]
        return match.group(0)

    text = RAW_COVER_RE.sub(raw_replacer, text)

    if text != original:
        path.write_text(text, encoding="utf-8")
    return stats


def main() -> int:
    print("Building dev.to URL map...")
    mapping = build_mapping()
    out = DOCS / "devto-url-map.json"
    out.write_text(json.dumps(mapping, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Mapped {len(mapping)} articles -> {out}")

    cover_cache = {}
    if COVER_CACHE.exists():
        cover_cache = json.loads(COVER_CACHE.read_text(encoding="utf-8"))

    totals = {"files": 0, "blob": 0, "canonical": 0, "raw_cover": 0}
    for md in sorted(DOCS.glob("devto*.md")):
        if md.name == "devto-article-catalog.md":
            continue
        stats = fix_file(md, mapping, cover_cache)
        if any(stats.values()):
            totals["files"] += 1
            totals["blob"] += stats["blob"]
            totals["canonical"] += stats["canonical"]
            totals["raw_cover"] += stats["raw_cover"]
            print(f"  {md.name}: blob={stats['blob']} canonical={stats['canonical']} cover={stats['raw_cover']}")

    print(
        f"Done: {totals['files']} files, "
        f"{totals['blob']} blob links, "
        f"{totals['canonical']} canonicals, "
        f"{totals['raw_cover']} cover URLs"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())