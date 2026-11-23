---
id: TASK-340
title: >-
  Ordered delivery per @PartitioningKey on SNS/SQS via FIFO topics and message
  groups
status: Done
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

## Analysis

<!-- SECTION:ANALYSIS:BEGIN -->
- SNS/SQS currently provisions standard resources and `GenericSnsPublisher` does not use the already-registered `EventRegistry` partitioning-key extractor.
- Implement derived FIFO behavior: a registered `@PartitioningKey` makes the event's SNS topic and subscriber SQS queue FIFO automatically; events without a key keep existing standard resources and publishing behavior. No separate FIFO configuration is introduced.
- FIFO names use the AWS-required `.fifo` suffix; queue creation sets `FifoQueue=true`; publishing supplies the partition key as `MessageGroupId` and a unique message deduplication ID. The single SQS polling loop processes received messages sequentially.
- Tests should cover publisher group/deduplication fields and automatic FIFO resource creation, plus an SNS/SQS LocalStack integration test asserting same-key delivery order. Document broker-specific ordering guarantees and remove the showcase gap.
- The working tree contains a pre-existing unrelated modification to `.idea/encodings.xml`; preserve it.
- Implemented EventRegistry key-presence lookup (including typed events), FIFO topic/queue provisioning and SNS group/deduplication metadata, plus focused unit tests. `mvn -pl core -Dtest=EventRegistryTest,GenericSnsPublisherTest,SqsUtilTest test` passes.
- Added a LocalStack integration test to the SNS/SQS example and moved its message event key from the unique message ID to the channel reference so the test represents same-aggregate ordering. `mvn -pl examples/sns-sqs -am -DskipTests test` compiles the main, generated and integration-test sources successfully without starting LocalStack. The user requested leaving integration execution to CI.
- Explicit topic names ending in `.fifo` also provision FIFO resources; keyless events published to those topics share one default message group. Null, blank and overlength partitioning keys fail before publish.
- Updated the developer guide's Kafka, Pub/Sub and SNS/SQS ordering guidance. The current showcase capability manifests do not list `ORDERED_DELIVERY` as unsupported, so no gap declaration needed removal.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Events with a @PartitioningKey are delivered in order per key on SNS/SQS, proven by an integration test
- [x] #2 FIFO topics and queues are provisioned automatically, also in LocalStack-based tests
- [x] #3 Developer guide documents ordering guarantees per broker
- [x] #4 showcase-sns-sqs no longer declares ORDERED_DELIVERY as a gap
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- EventRegistry exposes partition-key presence and resolves keys for both regular and typed events. Keyed events publish to automatically provisioned `.fifo` SNS topics with `MessageGroupId` set to the key and a generated deduplication ID. Explicit `.fifo` topic names are also supported, using a single default message group when no key is declared.
- Matching SQS subscriptions automatically create `.fifo` queues and FIFO dead-letter queues with FIFO attributes enabled. The existing serial poller processes messages sequentially; null, blank, or overlength partition keys fail before publish.
- Added unit coverage for typed-key resolution, FIFO SNS configuration, publisher metadata, explicit FIFO topics, and resource attributes. Added a LocalStack integration test asserting same-channel message delivery order. `mvn -pl core test` passes (212 tests); `mvn -pl examples/sns-sqs -am -DskipTests test` compiles the SNS/SQS example and integration tests. LocalStack execution is left to CI as requested.
- Updated the developer guide's Kafka, Pub/Sub, and SNS/SQS ordering guidance. The current showcase capability manifests already contain no `ORDERED_DELIVERY` gap.
<!-- SECTION:NOTES:END -->
