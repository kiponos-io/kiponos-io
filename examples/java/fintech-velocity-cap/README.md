# fintech-velocity-cap

Live transaction velocity limit (`max-tx-per-min`) from the Kiponos hub — change the cap without redeploying the payment jar.

## Hub

```text
examples/fintech-velocity-cap/max-tx-per-min = 60
```

## Run

```bash
cd examples/java/fintech-velocity-cap
cp kiponos.local.env.example kiponos.local.env   # from kiponos.io → Connect
./gradlew test run
```

Article draft: `docs/examples/medium-drafts/fintech-velocity-cap.md`
