# Super Pattern: Live State Machine (Order)

**Gang of Four:** State  
**Kiponos Super Pattern:** current state + allowed transitions live in the hub.

## Problem

Order transitions are enums and switch statements. Freezing “paid → cancelled” during an incident means a hotfix release.

## Super Pattern

```text
patterns / state / order / current = draft | paid | shipped | cancelled
patterns / state / order / allowed = draft>paid,paid>shipped,...
```

`tryTransition(next)` checks the live matrix, then updates `current`.

## Run

```bash
cd examples/java/pattern-state-live-order
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
./gradlew run --args='paid'
```

## Python parity

`examples/python/pattern-state-live-order/`

## Moral

**State machines describe what may happen next. Kiponos lets you rewrite “may” without rewriting the jar.**
