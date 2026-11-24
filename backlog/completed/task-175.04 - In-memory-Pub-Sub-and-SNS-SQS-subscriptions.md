---
id: TASK-175.04
title: In-memory Pub/Sub and SNS/SQS subscriptions
status: Done
assignee: []
created_date: '2026-11-23 11:00'
labels:
  - test
dependencies:
  - TASK-175.03
parent_task_id: TASK-175
priority: high
type: task
ordinal: 156400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Cover Pub/Sub and SNS/SQS with the in-memory bus (TASK-175 design §4-5, decision-011). Generated subscribers depend on the concrete `PubSubUtil`/`SqsUtil`, which need cloud clients.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Narrow subscription interfaces are extracted from `PubSubUtil` and `SqsUtil`, implemented by both, and generated subscribers depend on the interfaces
- [x] #2 Header/attribute stamping and reading move into per-platform message codecs in `core`, used by the real publishers/subscribers and by the bus, without behaviour change for the real transports
- [x] #3 In in-memory mode, Pub/Sub and SQS subscribers and `@TestEventConsumer` fields receive events through the bus, serialized with the platform serializers
- [ ] #4 The Pub/Sub and SNS/SQS examples run in memory, except tests marked `containers = true` (deferred to TASK-175.05, see notes)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- **Subscription SPI.** `PubSubSubscriptions` and `SqsSubscriptions` (core) each declare `subscribe(request)`; `PubSubUtil` and `SqsUtil` implement them. `PubSubSubscriberWriter` and `SqsSubscriberWriter` generate a constructor parameter of the interface type. Generated subscribers only ever call `subscribe(request)` on it, so nothing else had to move. The generated publishers shown in older expected-output fixtures are no longer written by the processor; the real publishers are `GenericPubSubPublisher` and `GenericSnsPublisher`. Expected-output fixtures and the two SpEL topic tests were updated.
- **Shared codecs.** The extraction is split by direction, because each real class owns only one half and keeping their constructors unchanged leaves every existing test and Spring wiring untouched: `PubSubMessageEncoder` (payload, `type`, version, `EventType.HEADER` attributes and ordering key; the key lookup is a function so the publisher keeps asking `PubSubUtil#keyFor`), `PubSubMessageDecoder` (concrete binding, typed, plain), `SnsMessageEncoder` (payload, subject and message attributes; FIFO group and deduplication ids stay in the publisher because they are transport concerns) and `SqsMessageDecoder`. `GenericPubSubPublisher`, `GenericSnsPublisher`, `PubSubUtil#consume` and `SqsUtil#processMessage` now call them. The serializer and deserializer classes were not touched.
- **No behaviour change for real transports.** Evidence: the existing core suite (244 tests including `GenericPubSubPublisherTest`, `GenericPubSubJsonTest`, `GenericSqsJsonTest`, `GenericSnsPublisherTest`, `SqsDeserializerVersioningTest`, `GenericPlatformDispatchTest`) passes with unchanged production-facing constructors. Only the two `consumeTyped` tests of `PubSubUtilTest` moved, to `PubSubMessageCodecTest`, because that method is now the decoder. New unit tests cover round trip, attributes, ordering key, generic events, version migration with `EventMigrator` and unknown event types for both platforms. Not verified against real brokers (no Docker here).
- **In-memory side.** `PubSubMessageCodec` and `SnsMessageCodec` (prefab-test) implement `MessageCodec` with the core encoders and the production deserializers. The SNS codec wraps the payload in the SNS notification envelope (`Message`, `Subject`, `MessageAttributes`), exactly what an SQS queue receives, so `SqsDeserializer` reads it. `InMemoryPubSubSubscriptions` and `InMemorySqsSubscriptions` subscribe on the bus per request topic, honour the event type or `EventType` binding and the consumer (and the raw consumer for Pub/Sub), and ignore subscription and queue names, dead-letter policy, retries and the executor. A subscription only receives messages of its own platform's codec. `InMemoryMessagingAutoConfiguration` has nested configurations conditional on `PubSubAdmin` and `SnsTemplate`; they build the serializer, deserializer (with the `EventMigrator` bean), codec and subscriptions beans, because `PubSubConfiguration` and `SnsConfiguration` are filtered out. They need the `JsonUtil` and `JsonMapper` beans, which every Prefab application has.
- **`@TestEventConsumer`.** `InMemoryEventConsumerExecutionListener` needed no change: an `EncodedMessage` carries the codec that produced it, so each message is decoded by the codec of its event's platform. Tests cover Pub/Sub and SNS topics, plain and generic. Like the Kafka path, a consumer receives every event on its topic regardless of the field's type argument.
- **Platform claim.** The real dispatchers do not distinguish platforms for plain classes (`canDispatch(Class)` is "registered in the registry"; only one platform's beans exist in a real application), but for concrete bindings they use `ConcreteEventSupport.usesPlatform(EventType, Platform)`: the declared `@Event(platform)` equals the platform or is `DERIVED`. `ConcreteEventSupport.usesPlatform(Class, Platform)` applies the same rule to classes, reading `@Event` from the class or the nearest supertype (so sealed subtypes work); a class without any declaration claims any platform. All three codecs use it. With `DERIVED` events and several codecs on the classpath the first codec wins, which is consistent with the processor: deriving the platform fails at compile time when more than one platform is present, so mixed applications must declare platforms.
- **AC #4 deferred.** Running the Pub/Sub and SNS/SQS examples in memory needs the in-memory repositories (TASK-175.02) and a Docker-free run, so it is verified in TASK-175.05. This task only verified that the whole reactor, including `examples/pubsub`, `examples/sns-sqs` and the showcase variants, compiles with the new generated code (`mvn install -DskipTests`).
- **Documentation.** User-facing documentation of the in-memory backend is written in TASK-175.05 (see TASK-175 plan); no public feature changed in this task apart from the `PubSubSubscriptions` and `SqsSubscriptions` interfaces, which are internal.
<!-- SECTION:NOTES:END -->
