---
id: TASK-266
title: >-
  Support @EventHandler methods with interface or sealed-supertype parameter for
  all handler variants
status: Done
assignee: []
created_date: '2026-07-23 12:13'
labels:
  - annotation-processor
  - events
  - kafka
  - pubsub
  - sns-sqs
dependencies: []
priority: medium
---

## Analysis

- The three handler variants are assembled in `annotation-processor` through:
  - `be.appify.prefab.processor.event.handler.StaticEventHandlerPlugin`
  - `be.appify.prefab.processor.event.handler.byreference.ByReferenceEventHandlerPlugin`
  - `be.appify.prefab.processor.event.handler.multicast.MulticastEventHandlerPlugin`
- Handler parameter validation is currently centralized in `EventHandlerPlugin#getEventType(...)`, which rejects parameters when `TypeManifest#asElement()` is `null` but otherwise does not distinguish concrete event records from interface or sealed supertype contracts.
- Consumer dispatch generation for Kafka / PubSub / SNS-SQS converges on `ConsumerWriterSupport`, especially:
  - `rootEventType(...)`
  - `eventTypeOf(...)`
  - `concreteEventTypes(...)`
  - `writeEventHandler(...)`
- Current polymorphic support is limited to `@Avsc` interfaces. `concreteEventTypes(...)` only expands `@Avsc` contract interfaces into generated record implementations, so non-`@Avsc` interface or sealed-supertype handler parameters are not expanded into concrete listener types for generated consumers.
- `StaticEventHandlerPlugin#findStaticCompanion(...)` and the instance-companion detection methods currently match parameter types by exact element equality, which likely prevents companion pairing when one handler is declared on a supertype and the matching create/update companion is declared on a concrete subtype.
- Existing regression coverage for handler variants lives in `annotation-processor/src/test/java/be/appify/prefab/processor/event/handler/EventHandlerWriterTest.java`. New tests should cover static, merged instance, by-reference, and multicast variants with an interface or sealed event contract parameter.

## Implementation Notes

- Added regression coverage for `@EventHandler` methods that declare an interface or sealed-supertype parameter across:
  - static aggregate handlers
  - merged handlers
  - `@ByReference` create-or-update handlers
  - `@Multicast` create-or-update handlers
  - generated Kafka, Pub/Sub, and SNS/SQS consumers
- Fixed `MulticastEventHandlerWriter` so `@Multicast(parameters = ...)` resolves zero-argument accessor methods on the event contract in addition to direct fields. This allows repository query arguments to be read from interface and sealed-supertype event contracts.
- Updated the create-or-update messaging fixtures so the shared event contract explicitly declares the reference accessor required by `@ByReference(property = ...)`.
- Updated `backlog/docs/annotation-reference.md`, `backlog/docs/feature-guides.md`, and `backlog/docs/troubleshooting.md` to document supertype handler parameters and accessor-based property mapping.

## Verification

- Focused regression suite:
  - `mvn -pl annotation-processor,kafka,pubsub,sns-sqs -am -Dtest=EventHandlerWriterTest,KafkaConsumerWriterTest,PubSubSubscriberWriterTest,SqsSubscriberWriterTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Full repository suite:
  - `mvn test`
  - `mvn -q test` (re-run with explicit success marker)

