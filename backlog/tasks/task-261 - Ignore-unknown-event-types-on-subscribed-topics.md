---
id: TASK-261
title: Ignore unknown event types on subscribed topics
status: Done
assignee: []
created_date: '2026-07-08 08:37'
updated_date: '2026-07-08 17:57'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When external producers publish new event types on a topic that this application already consumes, generated consumers must not fail processing. Unknown event types should be safely ignored so known event processing continues across Kafka, SNS/SQS, and Pub/Sub.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Kafka consumers continue running when a message with an unknown event type arrives on a subscribed topic.
- [x] #2 SNS/SQS consumers continue running when a message with an unknown event type arrives on a subscribed topic.
- [x] #3 Pub/Sub consumers continue running when a message with an unknown event type arrives on a subscribed topic.
- [x] #4 Unknown event messages are ignored and do not trigger user-defined handler methods for known event types.
- [x] #5 Automated tests cover unknown-event handling for Kafka, SNS/SQS, and Pub/Sub consumer paths and verify processing continues for subsequent known events.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented unknown event type tolerance end-to-end in runtime consumers/deserializers.

Code changes:
- Added `UnknownEventTypeException` in `core` to represent unsupported incoming event types.
- Kafka:
  - `DynamicDeserializer` now throws `UnknownEventTypeException` for AVRO schema/type mismatches and JSON invalid subtype ids.
  - `KafkaConfiguration` now uses a custom `ConsumerRecordRecoverer` that skips DLT publishing for `UnknownEventTypeException` and logs a warning instead.
- SNS/SQS:
  - `SqsDeserializer` now throws `UnknownEventTypeException` when SNS `Subject` type is not allowlisted.
  - `SqsUtil.processMessage` now catches `UnknownEventTypeException`, logs, acknowledges (deletes) the message, and continues.
- Pub/Sub:
  - `PubSubUtil.consumeTyped` now ignores unknown message `type` attributes (log + return) instead of throwing.

Tests:
- Updated `DynamicDeserializerTest` to assert `UnknownEventTypeException` for unresolved AVRO schemas.
- Added `SqsDeserializerTest` covering unknown type rejection and continued processing of subsequent known events.
- Extended `PubSubUtilTest` to verify unknown typed messages are ignored and subsequent known typed messages are still processed.
- Extended `KafkaConfigurationTest` to verify recoverer behavior: unknown events skip DLT, other exceptions delegate to DLT.

Documentation:
- Updated `backlog/docs/generated-artefacts.md` to document that generated consumers ignore unknown event types and keep processing known events.

Validation:
- Ran `mvn test -pl core` successfully (82 tests, 0 failures).
<!-- SECTION:NOTES:END -->
