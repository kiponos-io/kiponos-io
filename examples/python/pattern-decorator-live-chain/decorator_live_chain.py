#!/usr/bin/env python3
"""Super Pattern: Live Decorator Chain — Python parity."""
from __future__ import annotations
import os, sys, time
from typing import Callable

_REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
sys.path.insert(0, os.path.join(_REPO, "agent-kit"))

Request = tuple[str, str | None]  # path, body

def parse_chain(csv: str) -> list[str]:
    return [p.strip().lower() for p in (csv or "").split(",") if p.strip()]

def execute(chain_csv: str, path: str, retry_max: int = 2, cache_ttl: int = 30) -> tuple[str, str, list[str]]:
    chain = parse_chain(chain_csv)
    trace: list[str] = []
    def core(req: Request) -> Request:
        trace.append("core:" + req[0])
        return (req[0], f"OK body for {req[0]}")
    pipeline: Callable[[Request], Request] = core
    for layer in reversed(chain):
        inner = pipeline
        if layer == "metrics":
            def pipeline(req: Request, inner=inner) -> Request:  # type: ignore
                t0 = time.time()
                r = inner(req)
                trace.append(f"metrics:elapsedMs={int((time.time()-t0)*1000)}")
                return r
        elif layer == "retry":
            def pipeline(req: Request, inner=inner) -> Request:  # type: ignore
                attempts = 0
                while attempts <= retry_max:
                    attempts += 1
                    r = inner(req)
                    trace.append(f"retry:attempts={attempts}/max={retry_max}")
                    return r
                raise RuntimeError("retry exhausted")
        elif layer == "cache":
            def pipeline(req: Request, inner=inner) -> Request:  # type: ignore
                trace.append(f"cache:ttl={cache_ttl}s miss→inner")
                return inner(req)
        else:
            def pipeline(req: Request, inner=inner, layer=layer) -> Request:  # type: ignore
                trace.append(f"skip:unknown={layer}")
                return inner(req)
    out = pipeline((path, None))
    return ",".join(chain), out[1] or "", trace

def main() -> int:
    path = sys.argv[1] if len(sys.argv) > 1 else "/api/catalog"
    chain, retry, ttl = "metrics,retry", 2, 30
    try:
        from kiponos import Kiponos
        with Kiponos.connect() as k:
            base = "patterns/decorator/http-client"
            if k.get(f"{base}/chain") is None:
                k.set(f"{base}/chain", chain)
                k.set(f"{base}/retry-max", str(retry))
                k.set(f"{base}/cache-ttl-s", str(ttl))
            chain = str(k.get(f"{base}/chain") or chain)
            retry = int(k.get(f"{base}/retry-max") or retry)
            ttl = int(k.get(f"{base}/cache-ttl-s") or ttl)
            print("(live hub) decorator policy loaded")
    except Exception as ex:
        print(f"(offline demo) {ex.__class__.__name__}")
    applied, body, trace = execute(chain, path, retry, ttl)
    print("chain:", applied)
    print("body:", body)
    print("log:", " | ".join(trace))
    print("Java twin: examples/java/pattern-decorator-live-chain")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
