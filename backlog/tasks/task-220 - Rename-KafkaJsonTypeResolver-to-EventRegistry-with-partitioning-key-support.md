---
id: TASK-220
title: Rename KafkaJsonTypeResolver to EventRegistry with partitioning key support
status: Done
assignee: []
created: 2026-05-18
updated: 2026-05-18
---

## Description

Rework `KafkaJsonTypeResolver` to include a function to resolve the partitioning key from the event.
The class is not Kafka/JSON-specific — it serves as a central registry for all event types, topics,
and routing metadata. It is also more than a type resolver: it now holds key extractor functions.
Rename to `EventRegistry`.

## Acceptance criteria

- [x] `KafkaJsonTypeResolver` is renamed to `EventRegistry` in `prefab-core`
- [x] `EventRegistry` gains `registerType(topic, type, keyExtractor)` overload and `keyFor(event)` method
- [x] `KafkaEventTypeRegistrarWriter` generates key extractor lambda registration when `@PartitioningKey` is present
- [x] `KafkaProducerWriter` uses `eventRegistry.keyFor(event).orElse(null)` instead of inline key extraction
- [x] All references updated: `DynamicDeserializer`, `KafkaTopicResolver`, `StreamsConfiguration`, tests

## Implementation notes

- Created `EventRegistry` in `be.appify.prefab.core.kafka` with:
  - `<E> registerType(topic, Class<E>, Function<E, String>)` — registers type + partitioning key extractor
  - `Optional<String> keyFor(Object)` — resolves key via exact type or supertype match
- Deleted `KafkaJsonTypeResolver.java`; all consumers now depend on `EventRegistry`
- `KafkaEventTypeRegistrarWriter` calls `ConsumerWriterSupport.keyField()` at annotation-processing time
  and emits `event -> event.<field>()` lambda into the generated registrar
- Generated `KafkaProducer` now injects `EventRegistry` and delegates key resolution to it at runtime
  (no more inline key-access code in the producer)
- Updated expected test resource files and `backlog/docs/annotation-reference.md`
