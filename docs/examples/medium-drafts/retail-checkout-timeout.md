# Black Friday Did Not Care About Your PSP Timeout

*A traveler’s note from abandoned carts, limping payment rails, and the checkout number that should not wait for a release train.*

---

There is a particular kind of silence in a retail war room.

Not the calm of a quiet morning. The silence after someone says cart conversion is down and the dashboards agree. You can hear the AC. You can hear someone refresh New Relic. You can hear the product person try not to say **Black Friday** out loud like it is a curse.

Then engineering says the sentence I have heard in more than one language:

**“The PSP is slow. We need a higher timeout. But we have to redeploy.”**

Redeploy.

During peak.

To change an integer.

While carts become ghost stories.

---

## Architecture: live checkout knobs on the money path

Checkout is a **trust ceremony**. The control plane must be live; the data plane must stay local.

Who does what at peak:

- **Storefront / cart API** — starts the ceremony  
- **Checkout filter** — applies `timeout-ms` + `soft-fail-on-timeout` from **SDK memory**  
- **PSP** — external HTTPS, the slow friend  
- **Kiponos hub + dashboard** — ops raises timeout / soft-fail without rolling carts  
- **WebSocket → SDK cache** — delivers the new integer **before** the next checkout  

<!-- medium-img: diagram-checkout-path.png -->

Design clarity when the rail limps:

- **PSP healthy** — keep timeout tight (e.g. 3000ms); soft-fail off; fast path.  
- **PSP lagging** — **raise** timeout to 5000–8000ms; optional soft-fail on. Fewer false timeouts.  
- **PSP dying** — keep timeout high; soft-fail **on** so customers get a retry path instead of a hard 500.  
- **Mis-set to 50ms** — clamp (≥250ms). Guardrail against self-inflicted UX DDoS.  

Old world vs live hub:

- Raise PSP timeout: PR + deploy mid-sale → **dashboard edit**  
- Enable soft-fail: hope it was coded and shipped → **live flag**  
- Same JAR in all pods: flags drift across rollouts → **same tree fan-out**  

---

## Configuration hell wears a shopping bag

Checkout is not a feature. Checkout is a **trust ceremony**.

Customer already decided. Wallet is open. You are one slow HTTPS call from a sad face emoji and a support ticket.

Old world theater:

1. Discover `payment.timeout` is 3000ms and the PSP p99 is now 4200ms  
2. Open a PR  
3. Wait for pipeline while marketing refreshes revenue charts  
4. Deploy half the fleet  
5. Discover soft-fail was never implemented — timeouts still become hard 500s  
6. Write a postmortem titled “we will add a kill switch next quarter”  

I have watched excellent commerce engineers — people who can reason about idempotency keys without notes — reduced to archaeologists of their own ConfigMaps because **an integer was packaged as a release**.

Airports taught me that boarding groups are fiction until the gate agent decides. Retail taught me that **timeouts are fiction until the customer abandons**.

---

## The tree

```text
examples / retail-checkout-timeout /
  timeout-ms            = int (clamped 250ms..60s)
  soft-fail-on-timeout  = yes | no
```

- **`timeout-ms`** — how long the storefront waits on the PSP  
- **`soft-fail-on-timeout`** — retry / degrade instead of detonating the session  

That is the whole moral:

**Checkout posture is an operational decision, not a build artifact.**

---

## The example

**`examples/java/retail-checkout-timeout`** on [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)

Plain `main`. Unit tests for clamps and soft-fail wording. Wire the same reads into your filter tomorrow.

```bash
cd examples/java/retail-checkout-timeout
export KIPONOS_ID='…'
export KIPONOS_ACCESS='…'
./gradlew test run
```

Try it tonight:

1. Run the tests. Confirm clamps reject absurd timeouts and soft-fail wording is explicit.  
2. Raise `timeout-ms` in the hub while imagining PSP lag — no redeploy.  
3. Flip `soft-fail-on-timeout` to yes — carts get a second chance instead of a stack trace.  
4. Imagine peak Black Friday: would you still wait for CI to change that integer?  

Ops play: raise timeout during PSP lag; enable soft-fail so carts get a second chance instead of a stack trace.

---

## The moral

If peak traffic requires a redeploy to change a timeout, you do not have a checkout system.

You have a **scheduled vulnerability** that coincides with revenue.

**Ship the integer. Leave the jar alone.** Let the customer finish paying.

People should not have to ship a release to keep checkout honest.

---

*Example + tests: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/retail-checkout-timeout](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/retail-checkout-timeout)*
