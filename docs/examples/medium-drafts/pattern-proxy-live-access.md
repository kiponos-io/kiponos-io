# GoF Proxy as a Super Pattern: Admin API gate Without Redeploy

*A traveler’s note — classic shape, live policy tree.*

---

Close a sensitive path from the hub while the process stays up.

## Hub tree

```text
patterns / … / enabled + role-allow + rate-per-min
```

## Clone and run the golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-proxy-live-access
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full source + logic tests live on GitHub. Articles cite snippets; the repo is the product.

## Moral

**Close a sensitive path from the hub while the process stays up.**

---

*Example: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-proxy-live-access](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-proxy-live-access)*
