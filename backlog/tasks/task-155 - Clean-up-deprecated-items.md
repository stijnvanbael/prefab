---
id: TASK-155
title: Clean up deprecated items
status: In Progress
assignee: []
created_date: '2026-05-03 07:41'
updated_date: '2026-05-03 07:41'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Remove all deprecated classes, annotations, and methods that have accumulated over time. The following items are marked `@Deprecated` and should be removed, along with any usages migrated to the replacement API:

**Test module (test/):**
- `be.appify.prefab.test.pubsub.Subscriber` record → replaced by `EventConsumer`
- `be.appify.prefab.test.pubsub.TestSubscriber` annotation → replaced by `TestEventConsumer`
- `be.appify.prefab.test.pubsub.asserts.*` (PubSubAssertions, PubSubSubscriberAssert, PubSubSubscriberAssertTimeoutStep, PubSubSubscriberAssertWhereStep, PubSubAssertNumberOfMessagesStep) → replaced by `be.appify.prefab.test.asserts.*`
- `be.appify.prefab.test.kafka.TestConsumer` annotation → replaced by `TestEventConsumer`
- `be.appify.prefab.test.kafka.asserts.*` (KafkaAssertions, KafkaConsumerAssert, KafkaConsumerAssertTimeoutStep, KafkaConsumerAssertWhereStep, KafkaConsumerAssertNumberOfMessagesStep) → replaced by `be.appify.prefab.test.asserts.*`
- `be.appify.prefab.test.sns.SqsSubscriber` record → replaced by `EventConsumer`
- `be.appify.prefab.test.sns.TestSqsSubscriber` annotation → replaced by `TestEventConsumer`
- `be.appify.prefab.test.sns.asserts.*` (SqsAssertions, SqsSubscriberAssert, SqsSubscriberAssertTimeoutStep, SqsSubscriberAssertWhereStep, SqsAssertNumberOfMessagesStep) → replaced by `be.appify.prefab.test.asserts.*`
- Remove deprecated support code from `TestSubscriberExecutionListener`, `TestConsumerExecutionListener`, `TestSqsSubscriberExecutionListener`

**Core module (core/):**
- `be.appify.prefab.core.annotations.MongoMigration` annotation → replaced by `@DbMigration`

**Postgres module (postgres/):**
- `PrefabPersistentEntity.getIdColumn()` method (deprecated with `forRemoval = true`)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All deprecated test pubsub classes (Subscriber, TestSubscriber, PubSubAssertions and related assert steps) are removed
- [x] #2 All deprecated test kafka classes (TestConsumer, KafkaAssertions and related assert steps) are removed
- [x] #3 All deprecated test sns classes (SqsSubscriber, TestSqsSubscriber, SqsAssertions and related assert steps) are removed
- [x] #4 Execution listeners (TestSubscriberExecutionListener, TestConsumerExecutionListener, TestSqsSubscriberExecutionListener) no longer contain deprecated support code
- [x] #5 MongoMigration annotation is removed from core module
- [x] #6 PrefabPersistentEntity.getIdColumn() deprecated method is removed
- [x] #7 All usages of deprecated items are migrated to their replacements (e.g. MessageIntegrationTest uses org.assertj.core.api.Assertions.assertThat instead of KafkaAssertions)
- [x] #8 MongoMigrationPlugin no longer references the removed MongoMigration annotation
- [ ] #9 All existing tests pass after the cleanup
<!-- AC:END -->
