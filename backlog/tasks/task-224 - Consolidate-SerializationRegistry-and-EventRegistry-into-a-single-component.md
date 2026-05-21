---
id: TASK-224
title: Consolidate SerializationRegistry and EventRegistry into a single component
status: To Do
assignee: []
created_date: '2026-05-21 08:52'
labels:
  - refactor
  - core
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Background

`SerializationRegistry` (`core/util`) and `EventRegistry` (`core/kafka`) serve overlapping purposes: both act as topic-keyed catalogues that are populated at startup and queried at runtime to drive serialisation/deserialisation decisions.

- **`SerializationRegistry`** maps a topic → `Event.Serialization` format (JSON, Avro, …) and is consumed by `SnsSerializer`, `SqsDeserializer`, and Kafka infrastructure.
- **`EventRegistry`** maps topic → Java type, type → topic(s), holds key-extractor functions, maintains the Jackson type-resolver allowlist, and implements `JacksonJsonTypeResolver`.

Both follow the same registry pattern (init-time registration, runtime lookup, customizer hooks) and are injected together in several places (e.g. `KafkaTestAutoConfiguration`, `KafkaPrefabStreamsTopologyTest`). Keeping them separate forces consumers to depend on two beans, duplicates the registry abstraction, and complicates the mental model.

## Goal

Merge the two registries into one cohesive component (e.g. `PrefabEventRegistry` or keep the name `EventRegistry`) that:

1. Retains every existing capability of both registries.
2. Exposes a single bean that consumers depend on.
3. Preserves or improves the customizer mechanism (`SerializationRegistryCustomizer` equivalents).
4. Keeps the public API backwards-compatible where possible, or provides a clear migration path.

## Files of Interest

- `core/src/main/java/be/appify/prefab/core/util/SerializationRegistry.java`
- `core/src/main/java/be/appify/prefab/core/util/SerializationRegistryCustomizer.java`
- `core/src/main/java/be/appify/prefab/core/kafka/EventRegistry.java`
- `core/src/main/java/be/appify/prefab/core/spring/EnablePrefab.java` (imports both)
- `test/src/main/java/be/appify/prefab/test/kafka/KafkaTestAutoConfiguration.java`
- `streams/src/main/java/be/appify/prefab/streams/kafka/StreamsConfiguration.java`
- All generated Kafka producer/registrar artefacts (annotation-processor templates)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A single registry component owns both topic→serialization-format and topic→type mappings, key extractors, and the Jackson type-resolver allowlist
- [ ] #2 SerializationRegistryCustomizer (or its successor) still allows third-party modules to register entries at startup
- [ ] #3 No consumer class needs to inject more than one registry bean
- [ ] #4 All existing unit and integration tests pass without modification (or are updated to reflect the new API)
- [ ] #5 The annotation processor generates registrars that target the consolidated component
- [ ] #6 EnablePrefab and auto-configuration wiring is updated to register only the new bean
- [ ] #7 Javadoc on the new component explains its full responsibility
- [ ] #8 Developer guide docs updated to reflect the consolidated component
<!-- AC:END -->
