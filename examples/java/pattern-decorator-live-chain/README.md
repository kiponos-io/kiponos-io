# Super Pattern: Live Decorator Chain

**Gang of Four:** Decorator  
**Kiponos Super Pattern:** which wrappers apply (and their knobs) live in the hub.

## Problem

Decorator promises dynamic stacking of cross-cutting behavior. In practice the stack is frozen at bean construction: metrics always on, retry always 3, cache never added mid-incident.

## Super Pattern

```text
patterns / decorator / http-client / chain       = metrics,retry,cache
patterns / decorator / http-client / retry-max   = 2
patterns / decorator / http-client / cache-ttl-s = 30
```

`execute()` rebuilds the wrapper stack from the live CSV each call. Ops or a remote SDK can append `cache` or drop `retry` without redeploy.

## Run

```bash
cd examples/java/pattern-decorator-live-chain
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

## Moral

**Decorator was always about composition. Kiponos makes the composition list operational.**
