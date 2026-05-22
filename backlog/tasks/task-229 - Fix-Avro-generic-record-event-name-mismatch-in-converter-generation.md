---
id: TASK-229
title: Fix Avro generic-record event name mismatch in converter generation
status: Done
assignee: []
created_date: '2026-05-22 06:01'
updated_date: '2026-05-22 06:19'
labels:
  - avro
  - generator
  - tests
dependencies: []
references:
  - avro-processor/src/main/java
  - >-
    examples/avro/src/test/java/be/appify/prefab/example/avro/sale/SaleIntegrationTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement generator fix so GenericRecordToEventConverterWriter uses same Avro schema naming strategy as EventSchemaFactoryWriter, then verify SaleIntegrationTest passes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Generated generic-record converter matches schema factory event names, including normalized names.
- [x] #2 AvroSchema name override behavior remains supported in converter matching.
- [x] #3 `SaleIntegrationTest` passes in examples/avro module after regeneration/build.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented converter/schema-name alignment in avro-processor so generic-record event dispatch uses Avro schema names (`avroSchemaNameOf`) consistently, including `@AvroSchema(name=...)` overrides.

Added regression fixtures and assertions for schema-name override dispatch in `GenericRecordToEventConverterWriterTest` and test resources under `event/avro/schemanameoverride`.

After the converter fix, `SaleIntegrationTest` still failed; additional investigation showed no domain events were published to Kafka because `GenericKafkaProducer` was not guaranteed as a bean in current scanning setup.

Added explicit bean registration in `core/src/main/java/be/appify/prefab/core/kafka/KafkaConfiguration.java` with `@ConditionalOnMissingBean(GenericKafkaProducer.class)` and covered with `KafkaConfigurationTest.genericKafkaProducerBeanIsCreated()`.

Validation: `mvn -pl avro-processor -am test` PASS; `mvn -pl examples/avro -am -Dtest=SaleIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` PASS.

Commits: `1e35578d4fafa74a3f6b6a85b9f12965b1222f8e`, `fe0f5c38c1fb377ff8a340355bc78e8a52bedf0b`.
<!-- SECTION:NOTES:END -->
