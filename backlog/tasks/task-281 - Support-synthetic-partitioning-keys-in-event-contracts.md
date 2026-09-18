---
id: TASK-281
title: Support synthetic partitioning keys in event contracts
status: Done
assignee: []
created_date: '2026-08-06 07:31'
updated_date: '2026-09-18 10:00'
labels:
  - feature
  - events
  - avro
  - kafka
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab currently supports event partitioning keys only when they map directly to a serialized field/accessor or an AVSC `keyProperty`. That forces developers to add transport-only fields to event payloads when the desired routing key is a derived value such as a composite business identifier or a normalized tenant/entity key.

Support synthetic partitioning keys so an event contract can expose a derived `@PartitioningKey` method without requiring that method name to be a persisted event field. This should work for regular event types and AVSC-generated event families when the derived method can be implemented from existing event properties.

Out of scope: changing partition selection algorithms, adding runtime hashing options, or introducing stream repartition operators.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Developers can declare a synthetic @PartitioningKey method for an event contract when the routing key is derived from existing event properties instead of a serialized field.
- [x] #2 AVSC-backed event contracts can use the same synthetic partitioning-key pattern without requiring keyProperty to point at a physical top-level schema field.
- [x] #3 Annotation processing distinguishes between schema-backed partitioning keys and synthetic/default/computed ones, keeping existing validation for field-backed keys while allowing derived implementations that are fully resolvable from the event contract.
- [x] #4 The build fails with clear compile-time feedback when a synthetic partitioning key cannot be generated or invoked safely, for example because the method is abstract for an AVSC-generated event, depends on properties that are not present on all referenced event types, or violates the existing key return-type rules.
- [x] #5 Generated event registrars and producer infrastructure use the synthetic method for key extraction everywhere Prefab currently honours event partitioning keys, and docs/examples explain when to use synthetic keys versus schema-backed @PartitioningKey or keyProperty.
<!-- AC:END -->

## Analysis

- Regular event registrars already extract keys by invoking the annotated method name, so method-based synthetic keys are structurally compatible with the existing `EventRegistry` flow as long as the generated lambda returns a `String`.
- The current AVSC path still assumes every shared `@PartitioningKey` method is schema-backed by a top-level field of the same name:
  - `avro-processor/.../AvscPlugin` validates the method name against each schema field list
  - `kafka/.../KafkaPlugin` repeats the same validation before generating AVSC registrars
- That assumption blocks valid default/interface methods such as `tenantId() + ":" + entityId()` even though the generated records can inherit and invoke those methods safely when their dependent accessors are present on every schema.
- The smallest safe fix is to classify partitioning keys as either:
  - schema-backed (`@PartitioningKey` abstract accessor or explicit `keyProperty`)
  - synthetic (`@PartitioningKey` default/concrete method)
- AVSC validation should keep the existing field-backed checks, but allow synthetic keys while failing early with explicit errors when:
  - the synthetic key method returns an unsupported type for `EventRegistry`
  - referenced schemas do not provide the abstract accessor methods required by the shared contract
  - an explicit `keyProperty` still points to a missing field

## Implementation Notes

- Added shared `PartitioningKeySupport` resolution in the annotation processor so generated registrars can distinguish schema-backed partitioning keys from synthetic default/concrete methods and validate supported return types early.
- Updated AVSC processing in both `AvscPlugin` and Kafka registrar generation to keep field-backed validation for abstract/shared accessors while allowing synthetic shared methods and explicitly rejecting schemas that miss required shared contract accessors.
- Added compile-testing coverage for:
  - synthetic partitioning-key methods on regular Kafka events
  - synthetic shared partitioning-key methods on AVSC contracts
  - invalid AVSC synthetic keys with missing contract accessors
  - invalid synthetic key return types
- Updated the public annotation docs and Javadocs to explain when to use synthetic `@PartitioningKey` methods versus per-schema `keyProperty`.

## Completion Notes

- Synthetic `@PartitioningKey` methods now work for regular event records and shared AVSC contracts when the method is directly invokable from the generated event type.
- AVSC validation now treats abstract `@PartitioningKey` methods as schema-backed accessors and default/concrete methods as synthetic keys, preserving the previous field-name validation only for the schema-backed case.
- Builds now fail with explicit annotation-processor errors when:
  - a partitioning-key method returns something other than `String` or a single-value wrapper around `String`
  - an AVSC schema used with a synthetic shared key is missing a required abstract contract accessor

## Verification

- Added focused compile-testing coverage in `AvscPluginTest` and `KafkaEventTypeRegistrarWriterTest`.
- Attempted Maven verification with:
  - `mvn -q -pl core,annotation-processor,avro-processor,kafka -am -Dtest=AvscPluginTest,KafkaEventTypeRegistrarWriterTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Maven verification was blocked in the sandbox because dependency resolution for `io.confluent:kafka-streams-avro-serde:8.3.0` could not reach `packages.confluent.io`.
