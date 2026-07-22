# GoF Bridge as a Super Pattern: Notify implementor Without Redeploy

*A traveler’s note — classic shape, live policy tree.*

---

Abstraction stable; transport chosen live.

## Hub tree

```text
patterns / … / implementor = smtp|ses|sendgrid
```

## Clone and run the golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-bridge-live-implementor
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full source + logic tests live on GitHub. Articles cite snippets; the repo is the product.

## Moral

**Abstraction stable; transport chosen live.**

---

*Example: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-bridge-live-implementor](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-bridge-live-implementor)*
