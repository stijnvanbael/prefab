---
id: TASK-385
title: Stabilize Pub/Sub showcase history integration tests
status: In Progress
assignee: []
created_date: '2026-11-24T14:42:44Z'
updated_date: '2026-11-28T09:12:04Z'
labels:
  - events
  - testing
dependencies: []
references:
  - >-
    examples/showcase/src/test/java/be/appify/prefab/example/showcase/support/Shop.java
priority: medium
type: bug
ordinal: 210000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Running the full Maven reactor and the complete `examples/showcase-pubsub` module repeatedly times out in `CustomerOrderHistoryIntegrationTest`: the Pub/Sub subscriber receives a stale `OrderEvent.Cancelled` after its `CustomerOrderHistory` target is absent, retries, and leaves subsequent history events unprocessed. Running only `CustomerOrderHistoryIntegrationTest` in isolation passes. `Shop` already documents that Pub/Sub messages can outlive a test and fail when the next test has an empty table. Investigate test isolation and subscriber failure handling without weakening the production fail-and-retry behavior for genuinely missing projections.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The Pub/Sub showcase history integration tests pass when the complete showcase-pubsub module runs.
- [ ] #2 Late events from prior tests cannot terminate or starve the subscriber used by subsequent tests.
- [ ] #3 Missing projections in production retain the intended retry and dead-letter behavior.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-26: reproduced on `origin/main` (a9b1d9bc) with a full `mvn verify` in a fresh worktree: three tests of
`CustomerOrderHistoryIntegrationTest` time out (20 s / 30 s awaits, empty history). The same test passes 4/4 when run
alone with `-pl examples/showcase-pubsub -am -Dtest=CustomerOrderHistoryIntegrationTest`. Seen while verifying
TASK-310, whose generated sources for the Pub/Sub variant are identical to main's.

Analysis (2026-11-28): reproduced on current main. Root cause: in tests, PersistenceTestExecutionListener clears all CrudRepositories before each test while Pub/Sub subscriptions persist; events published by earlier tests (e.g. OrderEvent.Cancelled from OrderIntegrationTest) are still in flight/backlog. CustomerOrderHistory's @Multicast handlers then throw IllegalStateException("No aggregates found") which triggers PubSubUtil inline retry + nack + redelivery; the generated consumer runs on a single-threaded executor (concurrency=1), so a stale poison message repeatedly occupies the only worker and starves subsequent CustomerOrderHistory events; awaitility then times out. Also, message.ack() currently sits inside the retryable, so a consumer success could theoretically be re-run, and consume() relies on catching only RetryException; nothing guarantees a worker is never killed. Production semantic to retain (criterion 3): consumer failure -> retry -> nack -> Pub/Sub redelivery -> dead-letter topic; must NOT ack/drop genuinely missing projections. Plan: (A) harden PubSubUtil.consume (ack once outside retry; catch Throwable; always nack; never rethrow; quiet ack/nack; track in-flight); (B) test isolation in prefab-test: resetSubscriptions() in PubSubUtil (stop subscribers, quiesce in-flight, delete+recreate subscriptions with empty backlog, re-subscribe) + PubSubTestExecutionListener ordered before PersistenceTestExecutionListener in containers mode.
<!-- SECTION:NOTES:END -->
