---
id: TASK-337.07
title: 'Showcase Avro slice: Avro-serialized events'
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-20 19:48'
labels:
  - examples
  - avro
dependencies:
  - TASK-337.04
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 178000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace `examples/avro` with an optional source folder `examples/showcase/src/avro/java` (+ tests). Serialization is chosen per event in source (`@Event(serialization = AVRO)`), so Avro cannot be a config-only variant.

Cover: Avro-serialized events (e.g. sale/payment events), `@Avsc`/`@AvscFile` usage, schema registry in tests (`prefab.test.schema-registry.enabled`), prefab-avro-processor. Include the folder in the Kafka-based builds (showcase, showcase-mongodb). Pub/Sub and SNS/SQS write bare Avro binary without a writer schema, which cannot be decoded (TASK-275), so those variants declare AVRO as a gap linked to TASK-275 and include the folder once it is fixed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Avro events are produced and consumed in an integration test on Kafka
- [ ] #2 Avro slice is included in every variant whose broker supports Avro; limitations reference their gap task
<!-- AC:END -->
