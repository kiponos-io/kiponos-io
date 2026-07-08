---
title: "Schema Evolution Guardrails — Live Compatibility Flags (Java SDK)"
published: false
tags: architecture, data, java, python
description: Schema break flags in code vs registry. Kiponos holds compatibility strictness ops flips during migrations.
canonical_url: https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-dataops-schema-evolution-guardrails.md
main_image: https://files.catbox.moe/e166i8.jpg
---

Thursday 13:02 UTC. **Dual-write migration** week — `order-events` producers emit both v2 and v3 Avro schemas while consumers cut over gradually. One legacy inventory service still rejects v3 payloads because `STRICT_COMPAT = true` was compiled into its deserializer wrapper eighteen months ago.

The migration lead needs **`strict_mode: false`** on **that one consumer fleet** for forty-eight hours — not a company-wide schema registry policy change and not a redeploy during peak checkout. The platform engineer asks:

> "Compatibility strictness is a **migration posture flag** — why does relaxing it on one service require a **release train**?"

Most Java and Python event consumers encode schema guardrails as **constants**, **registry global policies**, and **identical strictness across all services**. [Kiponos.io](https://kiponos.io) holds per-service compatibility flags in profile `['schema']['prod']['evolution']` with **local `getBool()` on every deserialize**.

## The problem: strict_mode frozen in deserializer gates

```java
public class AvroEventDeserializer {
    private static final boolean STRICT_MODE = true;

    public OrderEvent deserialize(byte[] payload, String subject) {
        Schema readerSchema = schemaRegistry.getLatest(subject);
        if (STRICT_MODE && !compatibilityChecker.isBackwardCompatible(readerSchema, payload)) {
            throw new SchemaIncompatibleException(subject);
        }
        return avroDecoder.decode(payload, readerSchema);
    }
}
```

Registry policy is global — slow to change:

```yaml
schema:
  prod:
    evolution:
      strict_mode: true
```

During dual-write migration you need to:

1. Set **`services.inventory_consumer.strict_mode`** to `false` temporarily
2. Keep **`services.payments_consumer.strict_mode`** at `true`
3. Enable **`migration.dual_write_phase`** with allowed schema versions

Redeploying twelve consumer fleets mid-migration is **schema theater** — lag grows while PRs queue.

## What teams believe vs production reality

| Belief | Production reality |
|--------|-------------------|
| "Strict mode is a registry-wide policy" | Migration needs per-consumer posture for weeks |
| "We'll use schema registry compatibility levels" | JVM still reads local boolean at deserialize |
| "Relaxing strict mode is dangerous everywhere" | Dangerous globally — surgical locally during dual-write |
| "Feature flags handle migration" | Booleans per service fleet are ops trees — not cohort flags |
| "One strict flag for all consumers" | Payments and inventory have different cutover calendars |

## The Aha

**`strict_mode` is operational config** — it flips during dual-write migrations, emergency consumer recovery, and staged registry cutovers. It belongs in profile `['schema']['prod']['evolution']` with local `getBool()` on every deserialize call.

## What Kiponos.io is for schema evolution guardrails

[Kiponos.io](https://kiponos.io) shares one evolution tree across consumer fleets. Profile `['schema']['prod']['evolution']` hydrates each JVM; per-service overrides live under `services/`.

`afterValueChanged` logs strictness changes, notifies `#data-migrations`, and increments `schema_strict_mode_change_total`.

Registry remains source of truth for **schema definitions** — Kiponos owns **runtime enforcement posture**.

## Reference architecture

![Architecture diagram](https://files.catbox.moe/fft0q8.png)

## Config tree — evolution, services, migration, registry, audit

Five folders — `evolution`, `services`, `migration`, `registry`, `audit`:

```yaml
evolution/
  strict_mode: true
  reject_unknown_fields: true
  fail_on_version_skew: true
  enabled: true
services/
  inventory_consumer/
    strict_mode: true
    allowed_reader_versions: [2, 3]
  payments_consumer/
    strict_mode: true
    allowed_reader_versions: [3]
  analytics_consumer/
    strict_mode: false
    allowed_reader_versions: [2, 3, 4]
migration/
  dual_write_phase: false
  relax_inventory_strict: false
  phase_expires_at_ms: 0
registry/
  cache_ttl_ms: 60000
  fallback_to_previous_schema: true
audit/
  last_change_by: ""
  siem_forward_enabled: true
```

Profile path: `['schema']['prod']['evolution']`.

## Java integration: live schema guard + afterValueChanged

```java
import io.kiponos.sdk.Kiponos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LiveAvroEventDeserializer {
    private final Kiponos kiponos = Kiponos.createForCurrentTeam();
    private final SchemaRegistryClient registry;
    private final String serviceId;

    public LiveAvroEventDeserializer(
            SchemaRegistryClient registry,
            @Value("${spring.application.name}") String serviceId) {
        this.registry = registry;
        this.serviceId = serviceId.replace("-", "_");
        kiponos.afterValueChanged(change -> {
            log.info("Schema evolution delta: path={} value={}", change.path(), change.newValue());
            if (kiponos.path("audit").getBool("siem_forward_enabled")) {
                siemClient.emit("dataops_schema_evolution_change", change.path(), change.newValue());
            }
        });
    }

    public OrderEvent deserialize(byte[] payload, String subject, int writerVersion) {
        boolean strict = resolveStrictMode();
        Schema readerSchema = registry.getLatest(subject);

        if (strict && !compatibilityChecker.isBackwardCompatible(readerSchema, payload)) {
            throw new SchemaIncompatibleException(subject);
        }

        if (!isVersionAllowed(writerVersion)) {
            if (kiponos.path("evolution").getBool("fail_on_version_skew")) {
                throw new SchemaVersionNotAllowedException(writerVersion);
            }
            log.warn("Accepting non-allowed version {} in relaxed mode", writerVersion);
        }

        return avroDecoder.decode(payload, readerSchema);
    }

    private boolean resolveStrictMode() {
        var migration = kiponos.path("migration");
        if (migration.getBool("dual_write_phase")
            && migration.getBool("relax_inventory_strict")
            && "inventory_consumer".equals(serviceId)) {
            return false;
        }

        var servicePath = kiponos.path("services", serviceId);
        if (servicePath.exists()) {
            return servicePath.getBool("strict_mode");
        }
        return kiponos.path("evolution").getBool("strict_mode");
    }

    private boolean isVersionAllowed(int writerVersion) {
        var servicePath = kiponos.path("services", serviceId);
        if (servicePath.exists()) {
            return servicePath.getList("allowed_reader_versions").contains(writerVersion);
        }
        return true;
    }
}
```

Every `getBool()` and `getList()` on deserialize is **local memory** — microseconds vs registry HTTP + Avro decode.

## Real-world scenarios

| Scenario | Without live evolution tree | With Kiponos DataOps guardrails |
|----------|----------------------------|--------------------------------|
| Dual-write inventory cutover | Fleet redeploy to relax strict | `services/inventory_consumer/strict_mode: false` |
| Payments stays strict | Risky global registry change | Per-service keys isolated |
| Migration phase ends | Second deploy wave | `migration/dual_write_phase: false` live |
| Analytics needs forward compat | Forked deserializer code | `analytics_consumer/strict_mode: false` |
| Postmortem who relaxed strict | Git archaeology | Kiponos ACL + SIEM |

## Performance: strict_mode on deserialize hot path

- **One WebSocket per consumer JVM** — not registry + config HTTP per message
- **Strict resolve is ≤4 local reads** — nanoseconds vs Avro decode
- **Delta patches** — one service key without fleet restart
- **Migration flags** colocated with per-service overrides
- **Registry cache TTL** in same tree — coordinated refresh posture

## Compare to alternatives

| Approach | Per-service relax in hours | Consumer restart | Migration phase flags |
|----------|---------------------------|------------------|----------------------|
| Registry global policy | Slow — affects all | N/A | No |
| STRICT_COMPAT constant + deploy | No — fleet recycle | Required | No |
| Feature-flag SaaS | Per-cohort awkward | Partial | Limited |
| Env var per deployment | Drift across pods | Rolling restart | Awkward |
| **Kiponos SDK** | **Seconds** | **None** | **Yes** |

## When not to use Kiponos for schema evolution

| Boundary | Better home |
|----------|-------------|
| Avro/Protobuf schema definitions | Schema registry |
| Registry compatibility level (BACKWARD, FULL) | Confluent / Apicurio policy |
| Topic creation and partition counts | Kafka GitOps |
| Protobuf breaking-change detection in CI | Buf / protolock |
| Long-term schema documentation | Data catalog |

## Getting started (15 minutes)

1. [Create TeamPro at kiponos.io](https://kiponos.io) — profile `['schema']['prod']['evolution']`.
2. Add `io.kiponos:sdk-boot-3` to each Kafka consumer service.
3. Set `-Dkiponos="['schema']['prod']['evolution']"` and map `serviceId` to `services/` folder keys.
4. Replace `STRICT_MODE` constants with `resolveStrictMode()`.
5. Wire `afterValueChanged` Slack + SIEM notifications.
6. Drill: staging dual-write — relax one service `strict_mode` and confirm v3 payloads deserialize **without consumer restart**.

## Further reading

- [Developer Quickstart](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-getting-started-developer-guide.md)
- [Product tour](https://dev.to/kiponos/getting-started-with-kiponosio-p5k)
- [GETTING-STARTED.md](https://github.com/kiponos-io/kiponos-io/blob/master/docs/GETTING-STARTED.md)
- [github.com/kiponos-io/kiponos-io](https://github.com/kiponos-io/kiponos-io)
- Related: [Config schema versioning](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-config-schema-versioning.md)
- Related: [Strangler fig migration](https://github.com/kiponos-io/kiponos-io/blob/master/docs/devto-arch-strangler-fig-migration.md)

---

*Kiponos.io — schema definitions live in the registry; strict_mode lives in the tree.*