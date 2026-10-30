---
id: TASK-303
title: Support generic event types in all event related features
status: To Do
assignee: []
created_date: '2026-10-30 14:08'
labels:
  - events
  - generics
dependencies: []
references:
  - backlog/tasks/task-028 - Generic-parameters.md
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
As a developer modelling domain events, I want to declare events with type parameters (for example `record Changed<T>(Reference<T> id, T payload) implements Keyed<...>` or `Envelope<P>`), so that I can reuse a single event shape across aggregates without duplicating records per payload type.

Today event handling is keyed on the raw `Class` of the event. Parameterised events either fail at annotation processing time or lose their type arguments at runtime, so every event-related feature must be checked and made generic-aware.

### Features in scope
- Event publishing (`@Event`, topic resolution) and the event registry (`topicForType`, topic per concrete parameterisation).
- Event handlers (`@EventHandler`), including handlers declared for a concrete parameterisation (`Changed<Customer>`) and wildcard/supertype handlers.
- Serialization formats: JSON, Avro (schema generation, naming and registration for parameterised records), schema evolution and event versioning (TASK-274, TASK-301).
- Messaging platforms: Kafka, Pub/Sub, SNS/SQS, including topic/queue/subscription naming and provisioning.
- Streams: `PrefabStreams.from/to`, joins, aggregations, stores, the topology test API and message tracing (TASK-302).
- Generated artefacts: dispatch, subscribers, documentation and test fixtures from annotation processing.

### Concepts to settle in the analysis
- Event identity: how a parameterised event maps to a topic (one topic per erased type, or one per concrete parameterisation) and to a schema name.
- Runtime type capture: generic type information is erased, so the concrete parameterisation must be recoverable from handler signatures, stream definitions and published instances (for example via `ParameterizedType` or an explicit type token).
- Supported parameter forms: concrete type arguments only, or also wildcards and bounded parameters.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An analysis documents how generic events are identified (topic, schema name, registry key) and records the decisions as an ADR.
- [ ] #2 A parameterised event can be declared, published and handled for a concrete parameterisation with JSON serialization.
- [ ] #3 Avro schemas are generated and registered for parameterised events with deterministic, collision-free names.
- [ ] #4 Event handlers for different parameterisations of the same event type receive only their own events.
- [ ] #5 Kafka, Pub/Sub and SNS/SQS publish and consume generic events, including topic/queue provisioning.
- [ ] #6 Prefab streams (`from`, `to`, `join`, `aggregate`, processors) and the topology test API support generic events.
- [ ] #7 Event versioning and schema evolution work for generic events.
- [ ] #8 Unsupported generic forms fail at build time with a clear annotation processing error.
- [ ] #9 Non-generic events keep their current behaviour, topics and wire format.
- [ ] #10 Documentation and feature guides describe generic events and their limits.
<!-- AC:END -->
