---
id: TASK-289
title: Support SpEL topic expressions in Pub/Sub and SQS subscribers
status: Done
assignee: []
created_date: '2026-09-29 10:30'
labels:
  - events
  - bug
dependencies:
  - TASK-288
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while fixing TASK-288. `PubSubSubscriberWriter` and `SqsSubscriberWriter` only inject a
topic via `@Value` when it matches `\$\{.+}` (a whole-string property placeholder). SpEL
expressions (`#{...}`) and composite values (`prefix.${env}.user`) are passed to
`SubscriptionRequest` as string literals, so the subscriber subscribes to a topic literally
named after the expression. The same applies to a custom `deadLetterTopic`.

A SpEL expression may expand to multiple topics, so each resolved topic needs its own
subscription (e.g. inject a `String[]` and subscribe in a loop), mirroring the approach taken
in `EventTypeRegistrarWriter`.

Kafka consumers are not affected: `@KafkaListener(topics = ...)` resolves placeholders and SpEL
(including array/collection results) itself.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Pub/Sub subscribers resolve `${...}` and `#{...}` topics at runtime and subscribe to every resolved topic
- [x] #2 SQS subscribers resolve `${...}` and `#{...}` topics at runtime and subscribe to every resolved topic
- [x] #3 Custom dead-letter topics containing expressions are resolved at runtime
- [x] #4 Processor tests cover SpEL topics for both platforms
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Extracted `TopicExpressions.isExpression` (annotation-processor, `processor.event`): a topic containing `${`
  or `#{` is resolved by Spring. Used by `EventTypeRegistrarWriter`, `PubSubSubscriberWriter` and
  `SqsSubscriberWriter`.
- Subscribers inject expression topics as `@Value String[] {event}Topics` and subscribe once per resolved
  topic in a `for (var topic : ...)` loop. Literal topics are unchanged.
- Custom dead-letter topics containing an expression are injected as `String deadLetterTopic`. The parameter
  is now added once per constructor; before, a handler with several placeholder topics and a placeholder
  DLT generated a duplicate `deadLetterTopic` parameter.
- Pub/Sub: removed the per-topic `Executor` fields (never read); each subscription gets its own
  `Executors.newFixedThreadPool(concurrency)` inline, so expanded topics keep their own concurrency.
  SQS keeps its single shared executor.
- Tests: `PubSubSpelTopicTest` / `SqsSpelTopicTest` assert the generated source and run the generated
  subscriber in a Spring context against a mocked `PubSubUtil` / `SqsUtil`, verifying one subscription
  per resolved topic and the resolved DLT. `ProcessorTestUtil.classLoaderOf` loads compiled classes from
  the in-memory compilation. Added `mockito-core` as a test dependency to pubsub and sns-sqs.
- Expected subscriber fixtures regenerated from actual processor output.
- Pub/Sub uses one subscription name for all topics of a handler, so only the first topic actually gets
  a subscription. Pre-existing; needs a migration decision, tracked in TASK-290.
<!-- SECTION:NOTES:END -->
