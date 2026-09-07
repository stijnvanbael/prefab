---
id: TASK-274
title: Support schema evolution for JSON and Avro events
status: To Do
assignee: []
created_date: '2026-09-07 06:34'
labels:
  - feature
  - events
  - avro
  - avro-processor
  - annotation-processor
  - kafka
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab event contracts change over time, but consumers cannot reliably read messages written against an older or newer version of the contract.

On the JSON path a field added to an event leaves old messages with `null` (or `0` for primitives) in the new component, a renamed field is silently dropped because Jackson 3 ignores unknown properties, and a renamed event type fails type resolution so the message is skipped as an unknown event type.

On the Avro path `DynamicDeserializer` decodes with Confluent's `GenericAvroDeserializer`, which uses the writer schema only. No reader schema is supplied, so Avro's own schema resolution never runs and field defaults, aliases and type promotion are all inert. Generated `GenericRecord -> Event` converters read through `SchemaSupport.getField`, which yields `null` for absent fields, AVSC `default` values are applied only when seeding generated builders, and code-first generated schemas emit fields with no defaults at all. AVSC-generated event families inherit all of this, and additionally match schema names to Java types by simple name, so renaming a record breaks routing.

Introduce first-class schema evolution: a declarative annotation vocabulary on the event contract that both serializations honour (previous names for a field or event type, and a default value for a field added later), plus reader-schema-driven resolution on the Avro path so Avro's native evolution rules actually apply.

Out of scope: Avro payloads on Pub/Sub and SNS/SQS carry no writer schema or schema id and cannot participate in Avro schema resolution at all - tracked separately. Build-time backward-compatibility gating of schema changes is also out of scope.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A field added to an event contract can declare a default value that is applied when consuming messages written before that field existed, for JSON and for Avro, on both code-first and AVSC-generated events.
- [ ] #2 A renamed field or renamed event type can declare its previous name(s), and messages written under the old name still deserialize into the current contract - covering JSON property names, Avro field names, and event-type resolution on Kafka, Pub/Sub and SNS/SQS.
- [ ] #3 Generated Avro schemas carry the field defaults and aliases declared on the contract, and default and aliases declared in an AVSC file are honoured at read time rather than only when seeding generated builders.
- [ ] #4 Avro deserialization on Kafka performs real reader/writer schema resolution against the generated schema for the target event type, so defaults, aliases and Avro type promotion apply.
- [ ] #5 Compile-time validation rejects evolution declarations that cannot be satisfied - for example a default literal that does not match the component type, a previous name that collides with a current field or event type name, or a default on a component whose Avro schema cannot express one.
- [ ] #6 Regression tests cover reading old-format and new-format messages for JSON and Avro, for code-first and AVSC-generated events, and confirm that contracts without evolution declarations behave exactly as today.
- [ ] #7 backlog/docs/ documents the evolution annotations, the guarantees per serialization format and transport, and the known limitation for Avro over Pub/Sub and SNS/SQS.
<!-- AC:END -->
