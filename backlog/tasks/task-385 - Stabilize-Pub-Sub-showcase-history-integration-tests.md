---
id: TASK-385
title: Stabilize Pub/Sub showcase history integration tests
status: To Do
assignee: []
created_date: '2026-11-24T14:42:44Z'
updated_date: '2026-11-24T14:42:53Z'
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
