---
id: TASK-275
title: Make Avro payloads self-describing on Pub/Sub and SNS/SQS
status: To Do
assignee: []
created_date: '2026-09-07 06:34'
labels:
  - bug
  - events
  - avro
  - pubsub
  - sns-sqs
dependencies:
  - TASK-274
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`PubSubDeserializer.deserializeAvro` and `SqsDeserializer.deserializeAvro` construct `new GenericDatumReader<GenericRecord>()` with no schema and then call `read(null, decoder)`. The matching serializers write bare Avro binary with no writer schema and no schema id (`PubSubSerializer`, and `SnsSerializer`, which base64-encodes the payload). Binary Avro cannot be decoded without the writer schema, so this path cannot work for real payloads and it blocks Avro schema evolution on those transports (TASK-274).

Make the payload self-describing - either the Confluent wire format (magic byte plus registry schema id, reusing the registry client already present on the Kafka path) or Avro single-object encoding (`C3 01` plus CRC-64-AVRO fingerprint) resolved against a fingerprint-to-schema map built from the generated `*SchemaFactory` beans - and decode against a reader schema so the same resolution behaviour as Kafka applies.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Avro events published to Pub/Sub and to SNS/SQS carry enough schema identity for a consumer to recover the writer schema.
- [ ] #2 Consumers on both transports decode Avro payloads using the recovered writer schema together with the generated reader schema, so field defaults, aliases and type promotion behave as they do on Kafka.
- [ ] #3 The chosen encoding is documented in backlog/docs/, including any required configuration and the migration path for deployments already publishing the current format.
- [ ] #4 Integration tests cover round-trip publish and consume of an Avro event over Pub/Sub and over SNS/SQS, including a message written against an older schema version.
<!-- AC:END -->
