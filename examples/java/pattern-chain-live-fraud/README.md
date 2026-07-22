# Super Pattern: Live Chain of Responsibility (Fraud)

**Gang of Four:** Chain of Responsibility  
**Kiponos Super Pattern:** handler order + knobs live in the hub.

## Problem

Fraud checks (amount cap, geo blocklist, velocity) are wired in code. Reordering or retuning them means a release while attackers are already probing.

## Super Pattern

```text
patterns / chain / fraud / order              = amount-cap,geo,velocity
patterns / chain / fraud / amount-cap-cents    = int
patterns / chain / fraud / blocked-countries   = csv
patterns / chain / fraud / velocity-max        = int
```

`evaluate()` walks the live order; first reject wins. Ops or a remote SDK can reorder handlers without redeploy.

## Run

```bash
cd examples/java/pattern-chain-live-fraud
cp kiponos.local.env.example kiponos.local.env   # fill tokens
./gradlew test run
./gradlew run --args='25000 US 2'
```

## Python parity

`examples/python/pattern-chain-live-fraud/`

## Moral

**The chain was always about sequential judgment. Kiponos makes the sequence and the thresholds editable while money is still moving.**
