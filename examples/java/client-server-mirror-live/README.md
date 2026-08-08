# client-server-mirror-live

**Client Mirrors Server Truth — Without Putting Tokens in the Browser**

Same **Team** tree — four peers:

| Peer | Path |
|------|------|
| Java | `examples/java/client-server-mirror-live` |
| Python | `examples/python/client-server-mirror-live` |
| Node React | `examples/node/client-server-mirror-live-react` |
| Node Angular | `examples/node/client-server-mirror-live-angular` |

Hub leaf: `mirror/truth` (default `steady`)

```bash
export KIPONOS_ID=… KIPONOS_ACCESS=…
export KIPONOS="['MyApp']['1.0']['Dev']['base']"
./gradlew test && ./gradlew run
```

Story: `docs/examples/medium-drafts/client-server-mirror-live.md`
