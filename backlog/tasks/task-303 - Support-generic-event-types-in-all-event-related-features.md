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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Analysis (current state)
- Event identity is currently erased to `Class<?>`: `EventRegistry` registers topics, serializers, reader schemas and dispatch strategies by raw classes; generated event registrars and consumers also resolve raw types.
- Annotation processing already receives generic `TypeMirror` values, but event handler discovery compares `asElement()` (the raw declaration), and generated event serializers/registrars are built from `TypeManifest`. Generic event declarations and parameterized handler signatures therefore need end-to-end type handling, not only a runtime registry change.
- JSON consumers need the concrete target type when binding a message. Kafka Avro additionally needs a parameterisation-specific reader schema; the existing reader-schema registry is keyed by `Class<?>`.
- `@Event` declares static topics. Provisioning and subscriber generation use those topic strings, so parameterising event types must not silently change existing non-generic topic names or generated artefacts.
- The scope spans `core`, `annotation-processor`, `avro-processor`, `kafka`, `pubsub`, `sns-sqs`, `streams`, topology tests, examples and backlog documentation. TASK-274 supplies evolution/reader-schema foundations; TASK-301 supplies version/migration rules; TASK-302 supplies stream tracing metadata and topology tests.

### Design direction for the ADR
- Introduce one canonical, immutable event type key that represents the raw event declaration plus recursively canonicalised concrete type arguments. Use this key for parameterised registry entries, reader schemas, handler routing and version/migration lookup; keep the existing raw-class path for non-generic events.
- Retain the declared `@Event` topic for generic specialisations by default. Distinguish specialisations in message metadata and dispatch using the event type key, rather than inventing broker topic names. That preserves explicit topic configuration and makes provisioning consistent across Kafka, Pub/Sub and SNS/SQS.
- Capture generic handler signatures from annotation-processing `TypeMirror`s. Generic publication must carry an explicit type token when the runtime event instance cannot reveal its erased type arguments; define the exact source API and generated publisher shape in the ADR before implementation. Do not guess from payload field values.
- Generate deterministic Avro schema names from the raw schema identity plus a stable encoding/hash of the canonical type key. Detect collisions at processing time; do not rename schemas or alter wire formats for existing non-generic events.
- Initially support only fully concrete type arguments (including nested parameterized types). Reject raw generic events, unresolved type variables, wildcards and unsupported bounds with element-specific processor errors unless the ADR explicitly expands this set.
- The ADR must also decide how the type key appears in JSON/Avro payload metadata and transport headers/attributes, how Avro-on-Pub/Sub and SNS/SQS behave given TASK-275's lack of writer schemas, and whether event-version baselines/migrations are tracked per raw generic declaration or per concrete specialisation.

### Implementation steps
1. **Settle contract and record ADR.** Confirm supported type forms, explicit publication/type-token API, metadata encoding, topic policy, deterministic schema naming, duplicate/collision handling, version/evolution baseline identity, and transport limitations. Map each decision to acceptance criteria and retain current non-generic topic names and bytes.
2. **Add the canonical event type model.** Implement and unit-test the type-key representation, stable name/metadata encoding and equality semantics in `core`. Extend registry lookups for parameterised keys while preserving existing `Class<?>` overloads and behavior. Define explicit errors for missing or ambiguous generic identity.
3. **Teach annotation processing to preserve type arguments.** Update event and handler discovery, validation and generated dispatch/registrars to carry full `DeclaredType` arguments instead of reducing to `asElement()`. Add compile-testing fixtures for concrete, nested, raw, wildcard, bounded and unresolved forms; ensure unsupported forms fail with actionable diagnostics.
4. **Implement JSON publish/consume routing.** Carry the canonical key through event publication and transport metadata, bind JSON with the captured concrete target type, and select only handlers matching that key. Test two specialisations on the same topic, wildcard/supertype handlers, missing/unknown keys and non-generic regression behavior.
5. **Implement Avro specialization.** Substitute generic arguments into generated field schemas and converters, register the reader schema under the canonical key, and generate deterministic collision-checked Avro names. Add code-first tests and explicitly implement or reject generic `@Avsc` contracts according to the ADR. Verify schema evolution and versioning remain correct for specialised schemas.
6. **Integrate all transports and provisioning.** Update Kafka, Pub/Sub and SNS/SQS producers, deserializers, subscribers and topic/queue/subscription provisioning to propagate and resolve the same identity. Test round trips and isolation for each platform and document any Avro schema-resolution limitation inherited from TASK-275.
7. **Integrate streams and topology test APIs.** Preserve the key through `PrefabStreams.from/to`, join, aggregate, processors, stores and tracing metadata; make generic source/target type tokens explicit where erasure prevents inference. Add backend-neutral topology contract tests and Kafka topology tests for parameterisation-specific routing, state restoration and unchanged non-generic topology identity.
8. **Integrate versioning/evolution, finish docs and examples.** Extend TASK-274/TASK-301 validators, baselines, migrations and compatibility checks to use the chosen generic identity without changing non-generic contracts. Update annotation reference, generated artefact/event/streams guides and one end-to-end example; then run focused module tests followed by the full Maven suite.

### Verification gates
- Gate 1: ADR resolves the identity/API/compatibility decisions before implementation starts.
- Gate 2: one concrete generic event can be published, JSON-deserialized and routed exclusively to its concrete handler; a second specialisation on the same topic is not cross-delivered.
- Gate 3: Avro specialisations have deterministic distinct schema names and reader registrations, with collision and evolution tests.
- Gate 4: transport and stream contract tests pass for each supported path; generated artefacts for existing non-generic events and their topics/wire formats remain unchanged.
- Gate 5: unsupported forms fail at compile time with clear diagnostics, documentation/examples match supported behavior, and the complete Maven test suite passes.
<!-- SECTION:PLAN:END -->
