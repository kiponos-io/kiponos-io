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

Checkout is a **trust ceremony**. The control plane must be live; the data plane must stay local:

<!-- medium-img: diagram-checkout-path.png -->

| Component | Job at peak |
|-----------|-------------|
| **Storefront / cart API** | Starts the ceremony |
| **Checkout filter** | Applies `timeout-ms` + `soft-fail-on-timeout` from **SDK memory** |
| **PSP** | External HTTPS — the slow friend |
| **Kiponos hub + dashboard** | Ops raises timeout / soft-fail without rolling carts |
| **WebSocket → SDK cache** | Delivers the new integer **before** the next checkout |

### Decision table (design clarity)

| Condition | `timeout-ms` | `soft-fail-on-timeout` | Customer experience |
|-----------|--------------|------------------------|---------------------|
| PSP healthy | e.g. 3000 | `no` | Fast path, hard fail rare |
| PSP lagging | **raise** to 5000–8000 | optional `yes` | Fewer false timeouts |
| PSP dying | keep high | **`yes`** | Retry path instead of hard 500 |
| Mis-set to 50ms | clamped (≥250ms) | — | Guardrail against self-inflicted DDoS of the UX |

### Old world vs live hub

| Move | Old world | Kiponos |
|------|-----------|---------|
| Raise PSP timeout | PR + deploy mid-sale | Dashboard edit |
| Enable soft-fail | Hope it was coded + shipped | Live flag |
| Same JAR in all pods | Flags drift across rollouts | Same tree fan-out |

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

| Key | Role |
|-----|------|
| `timeout-ms` | How long the storefront waits on the PSP |
| `soft-fail-on-timeout` | Retry / degrade instead of detonating the session |

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

Ops play: raise timeout during PSP lag; enable soft-fail so carts get a second chance instead of a stack trace.

---

## The moral

If peak traffic requires a redeploy to change a timeout, you do not have a checkout system.

You have a **scheduled vulnerability** that coincides with revenue.

Ship the integer. Leave the jar alone. Let the customer finish paying.

---

*Example + tests: [github.com/kiponos-io/kiponos-io/tree/master/examples/java/retail-checkout-timeout](https://github.com/kiponos-io/kiponos-io/tree/master/examples/java/retail-checkout-timeout)*
