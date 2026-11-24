---
id: TASK-354
title: Document event pitfalls found by the agent benchmark
status: Done
assignee: []
created_date: '2026-11-21 09:45'
labels:
  - documentation
  - events
  - avro
  - agents
dependencies: []
priority: medium
type: docs
ordinal: 199000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Building the TASK-343 benchmark ran into event behaviour that the docs do not mention, or contradict:

1. **Sealed JSON event hierarchies need `@JsonTypeInfo(use = NAME)`.** Without it, consumers of the supertype (including `@TestEventConsumer EventConsumer<AccountEvent>`) cannot deserialize the events. The examples have the annotation; the feature guides never mention it.
2. **The `@TestEventConsumer` poison-pill loop.** A record that cannot be deserialized is retried and logged in a tight loop. One benchmark run wrote 575 MB of log in under a minute. It should fail fast or skip the record with a single clear message.
3. **`@Avsc` contracts must be sealed.** A non-sealed contract interface, as shown in the feature guide (7.4, `@AvscInterface` and synthetic-key examples), compiles but fails at startup with `Only sealed interfaces are supported for event types`.
4. **Adding a non-null column to a released schema** fails compilation with `Fatal error compiling: java.lang.IllegalStateException: Cannot add a NOT NULL column [stock] without a default value. Annotate the corresponding field with @DbDefaultValue`. The message is helpful, but it surfaces as a processor crash rather than a compiler error on the field (see TASK-323).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The event publishing guide explains @JsonTypeInfo for sealed JSON event hierarchies, or Prefab adds it automatically
- [x] #2 A test event consumer stops or skips an undeserializable record with one clear error instead of looping
- [x] #3 The AVSC docs and examples match what the runtime accepts (sealed contracts, or non-sealed support)
- [x] #4 The NOT NULL column error is reported as a compiler error on the field (coordinate with TASK-323)
<!-- AC:END -->

## Analysis

- 2026-11-24: The Kafka `@TestEventConsumer` listener caught deserialization failures without advancing the consumer offset, causing the same poison-pill record to be retried indefinitely. The AVSC runtime validates sealed event contracts through `EventRegistry`; the feature guide contained non-sealed examples. Database migration validation was thrown while constructing schema changes after field metadata had been reduced to a `Column`.

## Implementation Notes

- Documented `@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)` for sealed JSON event contracts and clarified that Prefab does not add it automatically.
- Updated AVSC examples and troubleshooting guidance to require sealed event contracts.
- Changed the Kafka test consumer to log and skip `RecordDeserializationException` records, and to stop on other polling failures instead of silently retrying.
- Preserved the originating field element in database column metadata and reported missing defaults through the annotation-processing `Messager` as a field-local compiler error.
- `mvn -pl annotation-processor -am -DskipTests compile` and `mvn -pl test -am -DskipTests compile` passed. The full annotation-processor test run had 579 passing tests and one unrelated pre-existing Windows path failure in `AgentContextPluginTest`.
