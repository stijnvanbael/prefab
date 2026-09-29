---
id: TASK-288
title: Support SpEL topic expressions in generated event type registrars
status: Done
assignee: []
created_date: '2026-09-29 10:00'
labels:
  - events
  - bug
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When the topic of an `@Event` is a Spring SpEL expression (e.g.
`@Event(topic = "#{'${topics.user}'.split(',')}")`), the generated `*EventTypeRegistrar`
registers the raw expression string as the topic name. At runtime the `EventRegistry`
then fails to find a serializer/deserializer for the actual topic.

`EventTypeRegistrarWriter` only recognised topics that are *entirely* a `${...}` property
placeholder (`topic.matches("\\$\\{.+}")`). Every other value — SpEL `#{...}` expressions
and composite values like `prefix.${env}.user` — was emitted as a string literal.

A SpEL expression may also expand to multiple topics (array, collection or
comma-separated string), so a single `String` injection is not sufficient.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Topics containing a `${...}` placeholder or a `#{...}` SpEL expression are injected with `@Value` into the generated registrar instead of registered literally
- [x] #2 An expression that expands to multiple topics registers the event type and serialization for each resolved topic
- [x] #3 Literal topics are still registered as string literals
- [x] #4 Annotation-processor tests cover SpEL topics, including a runtime check that the generated registrar populates the `EventRegistry` with the resolved topics
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. In `EventTypeRegistrarWriter`, treat a topic as an expression when it contains `${` or `#{`.
2. Inject every expression topic as a `String[]` constructor parameter. Spring converts SpEL
   array/collection results and comma-separated strings to `String[]`, so one expression can
   yield multiple topics.
3. In `customize`, loop over each injected array and register every resolved topic.
4. Add a Kafka processor test with a SpEL topic that asserts the generated source and loads the
   generated registrar in a Spring context to verify the resolved topics end up in the registry.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- `EventTypeRegistrarWriter.isExpression` treats any topic containing `${` or `#{` as an expression
  (previously only a whole-string `${...}` matched, so SpEL and composite topics were registered literally).
- Expression topics are injected as `String[]` fields named `{event}Topics` / `{event}Topics{index}`;
  Spring converts SpEL arrays/collections and comma-separated strings to `String[]`.
  `customize` loops over each array and registers every resolved topic. Literal topics are unchanged.
- Existing `${...}` topics now also use `String[]`, so a comma-separated property yields multiple topics.
- Tests: `KafkaEventTypeRegistrarWriterTest.spelTopicExpression*` (generated source and a Spring
  context runtime check). Expected registrar fixtures in kafka, pubsub and sns-sqs updated.
- Pub/Sub and SQS subscribers have the same limitation; tracked in TASK-289.
<!-- SECTION:NOTES:END -->
