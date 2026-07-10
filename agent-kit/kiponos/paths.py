"""Path helpers — bracket profile → JsonPath base; slash/dot paths."""

from __future__ import annotations

import re

VALUE_KEY = "value"


def profile_to_base_path(profile: str) -> str:
    """``['App']['rel']['env']['cfg']`` → ``$.rootAccount['apps']...``."""
    parts = re.findall(r"\['([^']*)'\]", profile or "")
    if len(parts) < 4:
        raise ValueError(
            "Invalid Kiponos profile. Expected "
            "['AppName']['Release']['Env']['ConfigName'], got: "
            f"{profile!r}"
        )
    app, rel, env, cfg = parts[0], parts[1], parts[2], parts[3]
    return (
        f"$.rootAccount['apps']['{app}']['rels']['{rel}']"
        f"['envs']['{env}']['cfgs']['{cfg}']"
    )


def join_json_path(base: str, *folders: str) -> str:
    path = base
    for f in folders:
        if not f:
            continue
        safe = f.replace("'", "\\'")
        path = f"{path}['{safe}']"
    return path


def parse_path(path: str) -> list[str]:
    """``a/b/c`` or ``a.b.c`` → segments. Empty → []."""
    path = (path or "").strip().strip("/")
    if not path:
        return []
    if "/" in path:
        return [p for p in path.split("/") if p]
    if "." in path:
        return [p for p in path.split(".") if p]
    return [path]
