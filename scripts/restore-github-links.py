#!/usr/bin/env python3
"""Point article links at GitHub when the file exists on origin/master; keep dev.to otherwise."""

from __future__ import annotations

import json
import re
from pathlib import Path

DOCS = Path(__file__).resolve().parents[1] / "docs"
GITHUB_BLOB = "https://github.com/kiponos-io/kiponos-io/blob/master"
GITHUB_RAW = "https://raw.githubusercontent.com/kiponos-io/kiponos-io/master"
COVER_CACHE = Path.home() / ".config/devto/cover-urls.json"

# dev.to articles with no docs/*.md on GitHub (or not pushed yet)
DEVTO_ONLY = {
    "https://dev.to/kiponos/getting-started-with-kiponosio-p5k",
    "https://dev.to/kiponos/kiponosio-developer-quickstart-java-python-and-your-first-live-config-change-3kjo",
}

DEVTO_RE = re.compile(r"https://dev\.to/kiponos/[a-z0-9-]+")
GITHUB_BLOB_RE = re.compile(
    r"https://github\.com/kiponos-io/kiponos-io/blob/master/(docs/[^\s\)\]\"']+\.md)"
)


def load_remote_docs() -> set[str]:
    cache = DOCS / "github-remote-docs.json"
    if cache.exists():
        paths = json.loads(cache.read_text(encoding="utf-8"))
    else:
        paths = []
    return {Path(p).name for p in paths if p.startswith("docs/") and p.endswith(".md")}


def load_remote_covers() -> set[str]:
    cache = DOCS / "github-remote-docs.json"
    paths = json.loads(cache.read_text(encoding="utf-8")) if cache.exists() else []
    return {
        Path(p).name
        for p in paths
        if p.startswith("docs/devto") and (p.endswith(".jpg") or p.endswith(".png"))
    }


def github_blob(fn: str) -> str:
    return f"{GITHUB_BLOB}/docs/{fn}"


def github_raw(fn: str) -> str:
    return f"{GITHUB_RAW}/docs/{fn}"


def main() -> int:
    devto_map = json.loads((DOCS / "devto-url-map.json").read_text(encoding="utf-8"))
    url_to_fn = {url.rstrip("/"): fn for fn, url in devto_map.items()}
    on_github = load_remote_docs()
    on_github_covers = load_remote_covers()

    cover_cache: dict[str, str] = {}
    if COVER_CACHE.exists():
        cover_cache = json.loads(COVER_CACHE.read_text(encoding="utf-8"))

    stats = {
        "files": 0,
        "canonical_github": 0,
        "canonical_devto": 0,
        "body_to_github": 0,
        "body_stay_devto": 0,
        "covers_github": 0,
        "covers_cdn": 0,
    }

    for md in sorted(DOCS.glob("devto*.md")):
        if md.name == "devto-article-catalog.md":
            continue

        text = md.read_text(encoding="utf-8")
        original = text
        fn = md.name

        if fn in on_github:
            canonical = github_blob(fn)
            stats["canonical_github"] += 1
        else:
            canonical = devto_map.get(fn, "")
            stats["canonical_devto"] += 1

        if canonical:
            text, _ = re.subn(
                r"^canonical_url:\s*.+$",
                f"canonical_url: {canonical}",
                text,
                count=1,
                flags=re.M,
            )

        def devto_replacer(match: re.Match[str]) -> str:
            url = match.group(0).rstrip("/")
            if url in DEVTO_ONLY:
                stats["body_stay_devto"] += 1
                return url
            target = url_to_fn.get(url)
            if target and target in on_github:
                stats["body_to_github"] += 1
                return github_blob(target)
            stats["body_stay_devto"] += 1
            return url

        text = DEVTO_RE.sub(devto_replacer, text)

        # normalize any stale github blob links for files now on github
        def blob_replacer(match: re.Match[str]) -> str:
            suffix = match.group(1)
            target = Path(suffix).name
            if target in on_github:
                return github_blob(target)
            # file not on remote — fall back to dev.to if mapped
            if target in devto_map:
                stats["body_stay_devto"] += 1
                return devto_map[target]
            return match.group(0)

        text = GITHUB_BLOB_RE.sub(blob_replacer, text)

        # main_image: GitHub raw when cover exists remotely, else CDN cache
        m = re.search(r"^main_image:\s*(.+)$", text, flags=re.M)
        if m:
            cover_name = None
            raw = m.group(1).strip()
            if "docs/" in raw:
                cover_name = Path(raw.split("docs/")[-1]).name
            elif raw.startswith("https://files.catbox.moe") or raw.startswith("https://litter.catbox.moe"):
                # try infer from article filename
                stem = fn.replace(".md", "").replace("devto-", "devto-cover-", 1)
                for ext in (".jpg", ".png"):
                    candidate = stem + ext
                    if (DOCS / candidate).exists():
                        cover_name = candidate
                        break

            new_cover = None
            if cover_name and cover_name in on_github_covers:
                new_cover = github_raw(cover_name)
                stats["covers_github"] += 1
            elif cover_name:
                local = DOCS / cover_name
                key = str(local.resolve())
                if key in cover_cache:
                    new_cover = cover_cache[key]
                    stats["covers_cdn"] += 1

            if new_cover and new_cover != raw:
                text = re.sub(
                    r"^main_image:\s*.+$",
                    f"main_image: {new_cover}",
                    text,
                    count=1,
                    flags=re.M,
                )

        if text != original:
            md.write_text(text, encoding="utf-8")
            stats["files"] += 1
            print(f"  {fn}")

    print(
        f"Done: {stats['files']} files | "
        f"canonical github={stats['canonical_github']} devto={stats['canonical_devto']} | "
        f"body ->github={stats['body_to_github']} stay-devto={stats['body_stay_devto']} | "
        f"covers github={stats['covers_github']} cdn={stats['covers_cdn']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())