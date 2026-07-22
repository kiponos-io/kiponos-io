# GoF Abstract Factory as a Super Pattern: Region family Without Redeploy

*A traveler’s note — classic shape, live policy tree.*

---

Currency + tax family selected live for the region you are in today.

## Hub tree

```text
patterns / … / family = us|eu
```

## Clone and run the golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-abstract-factory-live-region
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full source + logic tests live on GitHub. Articles cite snippets; the repo is the product.

## Moral

**Currency + tax family selected live for the region you are in today.**

---

*Example: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-abstract-factory-live-region](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-abstract-factory-live-region)*
