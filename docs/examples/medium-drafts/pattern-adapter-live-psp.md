# GoF Adapter as a Super Pattern: PSP Without Redeploy

*A traveler’s note — classic shape, live policy tree.*

---

Swap payment adapters without shipping a new jar.

## Hub tree

```text
patterns / … / provider = stripe|adyen|braintree
```

## Clone and run the golden example

```bash
git clone https://github.com/kiponos-io/kiponos-io.git
cd kiponos-io/examples/java/pattern-adapter-live-psp
cp kiponos.local.env.example kiponos.local.env
./gradlew test run
```

Full source + logic tests live on GitHub. Articles cite snippets; the repo is the product.

## Moral

**Swap payment adapters without shipping a new jar.**

---

*Example: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-adapter-live-psp](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/pattern-adapter-live-psp)*
