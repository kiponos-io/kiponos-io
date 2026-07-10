#!/usr/bin/env python3
"""Minimal CLI for the Kiponos Python SDK."""

from __future__ import annotations

import argparse
import json
import sys

from .client import Kiponos, KiponosError


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(prog="kiponos-cli", description="Kiponos live config CLI")
    p.add_argument("--profile", default=None, help="Bracket profile (or env KIPONOS)")
    sub = p.add_subparsers(dest="cmd", required=True)

    sub.add_parser("status")
    g = sub.add_parser("get")
    g.add_argument("path")
    s = sub.add_parser("set")
    s.add_argument("path")
    s.add_argument("value")
    l = sub.add_parser("list")
    l.add_argument("path", nargs="?", default="")
    d = sub.add_parser("dump")
    d.add_argument("path", nargs="?", default="")
    m = sub.add_parser("mkdir")
    m.add_argument("path")

    args = p.parse_args(argv)
    try:
        with Kiponos.connect(profile=args.profile, quiet=True) as k:
            if args.cmd == "status":
                print(
                    json.dumps(
                        {
                            "ok": True,
                            "team_id": k.team_id,
                            "profile": k.profile,
                            "root_keys": len(k.list_keys()),
                            "root_folders": k.list_folders(),
                        },
                        indent=2,
                    )
                )
            elif args.cmd == "get":
                v = k.get(args.path)
                if v is None:
                    print("null")
                    return 1
                print(v)
            elif args.cmd == "set":
                print(k.set(args.path, args.value))
            elif args.cmd == "list":
                print(
                    json.dumps(
                        {
                            "folders": k.list_folders(args.path),
                            "keys": k.list_keys(args.path),
                        },
                        indent=2,
                    )
                )
            elif args.cmd == "dump":
                print(json.dumps(k.dump(args.path), indent=2))
            elif args.cmd == "mkdir":
                print(k.mkdir(args.path))
        return 0
    except KiponosError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
