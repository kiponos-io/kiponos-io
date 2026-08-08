# canary-fullstack-live

**Canary Percent That Moves BFF, Workers, and UI Mirrors Together**

Same **Team** tree — four peers:

| Peer | Path |
|------|------|
| Java | `examples/java/canary-fullstack-live` |
| Python | `examples/python/canary-fullstack-live` |
| Node React | `examples/node/canary-fullstack-live-react` |
| Node Angular | `examples/node/canary-fullstack-live-angular` |

Hub leaf: `release/canary-percent` (default `5`)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
./gradlew test && ./gradlew run
```

Story: `docs/examples/medium-drafts/canary-fullstack-live.md`
