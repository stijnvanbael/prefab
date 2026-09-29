---
id: TASK-289
title: Support SpEL topic expressions in Pub/Sub and SQS subscribers
status: To Do
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
- [ ] #1 Pub/Sub subscribers resolve `${...}` and `#{...}` topics at runtime and subscribe to every resolved topic
- [ ] #2 SQS subscribers resolve `${...}` and `#{...}` topics at runtime and subscribe to every resolved topic
- [ ] #3 Custom dead-letter topics containing expressions are resolved at runtime
- [ ] #4 Processor tests cover SpEL topics for both platforms
<!-- AC:END -->
