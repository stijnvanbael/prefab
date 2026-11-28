---
id: TASK-385
title: Stabilize Pub/Sub showcase history integration tests
status: Done
assignee: []
created_date: '2026-11-24T14:42:44Z'
updated_date: '2026-11-28T09:37:33Z'
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

Implemented. Root cause: persistence is cleaned before every test while Pub/Sub subscriptions persist and retain unprocessed messages; stale events (e.g. OrderEvent.Cancelled) target missing projections, throw, trigger inline retry + nack + redelivery, and on the single-threaded (concurrency=1) subscriber starve later valid events. Fix has two parts, production retry/dead-letter semantics untouched. (A) prefab-core PubSubUtil.consume hardened: consumer success acked exactly once outside the retryable (never re-invoked after success); consumer failure -> retry policy -> nack for Pub/Sub redelivery + dead-letter; nothing rethrown; catch Throwable; ack/nack quiet; dispatch tracks in-flight and can never terminate/starve the executor. (B) test isolation: PubSubUtil.resetSubscriptions() stops subscribers, quiesces in-flight, discards backlog, recreates subscription (ordering + retry + DLT policy intact) and re-subscribes; new prefab-test PubSubTestExecutionListener (ordered before PersistenceTestExecutionListener, containers mode only) registered in META-INF/spring.factories. Missing projections in production keep fail-and-retry + dead-letter (@Multicast.failWhenEmpty unchanged); not ack-dropped. Criterion 1: full examples/showcase-pubsub 'mvn verify' passes repeatedly + -am. Tests: PubSubUtilConsumeTest, PubSubUtilResetSubscriptionsTest, PubSubTestExecutionListenerTest. Docs: backlog/docs/configuration.md. Pre-existing unrelated examples/showcase-sns-sqs CustomerOrderHistoryIntegrationTest flakiness tracked as TASK-406. No annotation/config/public generated artefact change; no cheat-sheet/recipe/benchmark change.
<!-- SECTION:NOTES:END -->
