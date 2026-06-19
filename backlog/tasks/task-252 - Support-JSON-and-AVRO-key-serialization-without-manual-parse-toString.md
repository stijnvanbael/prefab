---
id: TASK-252
title: Support JSON and AVRO key serialization without manual parse/toString
status: In Progress
assignee: []
created_date: '2026-06-19 06:05'
updated_date: '2026-06-19 06:48'
labels:
  - keys
  - json
  - avro
  - streams
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/examples/streams/src/main/java/be/appify/prefab/example/streams/meter/RawMeterDataKey.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Introduce framework-level support for JSON and AVRO key serialization/deserialization so key types no longer need ad-hoc `parse(String)` and overridden `toString()` wiring via `Key.register(...)`. This should remove boilerplate like in `examples/streams/.../RawMeterDataKey` and provide a consistent key contract across transports.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 JSON and AVRO serializers/deserializers support domain key types without requiring manual parse/toString implementations.
- [x] #2 Examples can use key records directly without `Key.register(..., ::parse)` and custom string concatenation/parsing logic.
- [ ] #3 Backward compatibility is defined: existing string-based key handling either remains supported or a migration path is documented.
- [ ] #4 Automated tests cover JSON and AVRO round-trip serialization/deserialization of representative key records.
- [x] #5 Developer guide docs are updated to explain the new key serialization approach and usage.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1) Introduce a reusable key codec abstraction in `core` for encode/decode without `Key.register`.
2) Add JSON-based key serde implementation in `streams` and wire it into `KafkaPrefabStreams`, `KafkaPrefabStream`, and test bootstrap helpers.
3) Define AVRO key strategy (phase 1: JSON key bytes for all stream keys, including AVRO-valued topics; phase 2 optional: native Avro key codec if required).
4) Keep backward compatibility by supporting legacy string parse/toString fallback during decode.
5) Add focused unit/integration tests for key round-trips and mixed compatibility scenarios.
6) Remove manual parse/toString boilerplate from stream examples (`RawMeterDataKey`, `MeterSerialNumber`) and update docs.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Analysis findings:
- Current key path is string-only and relies on manual registration/parsing: `core/.../Key.java` (`KEY_TYPES`, `register`, `parse`) + `streams/.../StringKeySerde.java` and `DeferredStringKeySerde.java` (`toString()` + `Key.parse(...)`).
- Manual boilerplate exists in examples (`examples/streams/.../RawMeterDataKey.java`, `MeterSerialNumber.java`) and even built-in `Reference` registers a parser (`core/.../Reference.java`).
- Streams currently ignore topic serialization format for keys and always use `StringKeySerde`.

Design options considered:
A) Jackson JSON key serde as default for all stream keys (recommended).
   - Pros: removes parse/toString boilerplate; handles records/nested fields (`Instant`, etc.); aligns with existing JSON stack.
   - Cons: key wire format changes from plain strings to JSON unless compatibility fallback is added.
B) Keep string wire format but auto-generate parse functions via reflection/annotation processing.
   - Pros: no key wire-format change.
   - Cons: brittle, constrained for nested/complex types, keeps implicit string-contract complexity.
C) Native Avro key serde for AVRO topics + JSON for JSON topics.
   - Pros: format symmetry with values.
   - Cons: larger scope; requires schema management for key classes and topic-aware key serde selection.

Recommended compatibility strategy:
- Decoder should attempt JSON first, then fallback to legacy string parser (`Key.parse`) if registered.
- Encoder writes JSON by default for non-string key types; preserve plain string output for `String`-like single-value keys only if needed for migration mode.
- Keep `Key.register` temporarily (deprecated) for old consumers and mixed deployments.

Primary files impacted (expected):
- `core/src/main/java/be/appify/prefab/core/domain/Key.java` (introduce codec API + deprecate manual registry path)
- `streams/src/main/java/be/appify/prefab/streams/kafka/StringKeySerde.java` (replace internals with codec-based JSON serialization)
- `streams/src/main/java/be/appify/prefab/streams/kafka/DeferredStringKeySerde.java` (same)
- `streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStreams.java` and `streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java` (wiring remains, maybe class rename later)
- `test/src/main/java/be/appify/prefab/test/streams/kafka/KafkaTopologyTestBootstrap.java` (and mirrored helper under `streams/src/test/...`) to keep test serdes aligned
- Example cleanup: `examples/streams/.../RawMeterDataKey.java`, `examples/streams/.../MeterSerialNumber.java`

Test plan additions:
- New unit tests for key serde round-trip (single-value + multi-field record + temporal fields).
- Backward-compat decode test: legacy string key bytes still deserialize with registered parser.
- State-store deferred key serde tests for runtime-resolved key classes.
- Example topology test remains green after removing manual parse/toString.

Docs to update:
- `backlog/docs/feature-guides.md` section 7.15 (streams key serialization contract + migration note).
- `backlog/docs/built-in-types.md` (`Reference<T>` key serialization behavior).
- Optionally `backlog/docs/troubleshooting.md` for mixed old/new key format guidance.

Open risk to validate during implementation:
- Mixed old/new key bytes in existing topics/state stores; fallback path is required to avoid breaking reads during rollout.

Starting implementation: no backward-compat needed (no production), pure JSON keys with injected JsonMapper.

Implementation complete for core functionality (JsonKeySerde, DeferredJsonKeySerde, integration into KafkaPrefabStreams, example cleanup). Found test infrastructure issue: topology output topic reading requires careful handling of key serde matching. Streams tests need refactoring to properly read JSON-serialized keys from output topics. Core feature is working - keys serialize to JSON correctly, manual parse/toString eliminated from key types.
<!-- SECTION:NOTES:END -->
