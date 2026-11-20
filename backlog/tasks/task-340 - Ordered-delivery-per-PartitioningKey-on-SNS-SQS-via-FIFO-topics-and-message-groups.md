---
id: TASK-340
title: >-
  Ordered delivery per @PartitioningKey on SNS/SQS via FIFO topics and message
  groups
status: To Do
assignee: []
created_date: '2026-11-20 19:48'
labels:
  - sns-sqs
  - events
  - coverage-gap
dependencies: []
priority: medium
type: feature
ordinal: 185000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
On Kafka, `@PartitioningKey` becomes the record key (per-key ordering); on Pub/Sub, `GenericPubSubPublisher` sets it as the ordering key (`pubSubUtil.keyFor(event).ifPresent(messageBuilder::setOrderingKey)`). On SNS/SQS, `GenericSnsPublisher.publishToTopics` sends to standard topics with only a subject and version headers: the partitioning key is ignored and `SqsUtil.ensureTopicExists` never creates FIFO topics/queues. Events for the same aggregate can therefore be processed out of order, which forces handlers to cope with it (cf. the optimistic-lock retry storms of TASK-199).

Support FIFO: when an event has a `@PartitioningKey` (or FIFO is configured), create `.fifo` topics and queues, publish with `MessageGroupId` = partitioning key and a `MessageDeduplicationId` (event/message ID), and make the SQS listener respect group ordering. Decide whether FIFO is opt-in per event or derived; mind FIFO throughput limits and the LocalStack test setup.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Events with a @PartitioningKey are delivered in order per key on SNS/SQS, proven by an integration test
- [ ] #2 FIFO topics and queues are provisioned automatically, also in LocalStack-based tests
- [ ] #3 Developer guide documents ordering guarantees per broker
- [ ] #4 showcase-sns-sqs no longer declares ORDERED_DELIVERY as a gap
<!-- AC:END -->
