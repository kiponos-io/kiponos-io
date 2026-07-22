# GoF Facade as a Super Pattern: Checkout facade Without Redeploy

*A traveler’s note — classic shape, live policy tree.*

---

One checkout() API; guts knobs live behind it.

## Hub tree

```text
patterns / … / tax-bps + inventory-check + notify
```

## Clone and run the golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-facade-live-knobs
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full source + logic tests live on GitHub. Articles cite snippets; the repo is the product.

## Moral

**One checkout() API; guts knobs live behind it.**

---

*Example: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-facade-live-knobs](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-facade-live-knobs)*
