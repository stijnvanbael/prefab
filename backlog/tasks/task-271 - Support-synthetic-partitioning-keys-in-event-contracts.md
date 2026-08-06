---
id: TASK-271
title: Support synthetic partitioning keys in event contracts
status: To Do
assignee: []
created_date: '2026-08-06 07:31'
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
- [ ] #1 Developers can declare a synthetic @PartitioningKey method for an event contract when the routing key is derived from existing event properties instead of a serialized field.
- [ ] #2 AVSC-backed event contracts can use the same synthetic partitioning-key pattern without requiring keyProperty to point at a physical top-level schema field.
- [ ] #3 Annotation processing distinguishes between schema-backed partitioning keys and synthetic/default/computed ones, keeping existing validation for field-backed keys while allowing derived implementations that are fully resolvable from the event contract.
- [ ] #4 The build fails with clear compile-time feedback when a synthetic partitioning key cannot be generated or invoked safely, for example because the method is abstract for an AVSC-generated event, depends on properties that are not present on all referenced event types, or violates the existing key return-type rules.
- [ ] #5 Generated event registrars and producer infrastructure use the synthetic method for key extraction everywhere Prefab currently honours event partitioning keys, and docs/examples explain when to use synthetic keys versus schema-backed @PartitioningKey or keyProperty.
<!-- AC:END -->
