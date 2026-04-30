---
id: TASK-153
title: Fix flaky CategoryStatsIntegrationTest in mongodb-example
status: To Do
assignee: []
created_date: '2026-04-30 18:14'
updated_date: '2026-04-30 18:14'
labels:
  - bug
  - flaky-test
  - mongodb-example
dependencies: []
priority: medium
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`CategoryStatsIntegrationTest.updateProductCountOnProductCreated` intermittently times out in CI.
The test publishes a `CategoryCreated` event and then waits (via Awaitility) for the Kafka consumer
to update `CategoryStats`, but the current 5-second timeout is too tight for CI runners under load.
The failure is a false negative: the production code is correct but the consumer simply hasn't
finished processing by the time the assertion fires.

Example failure:
```
CategoryStatsIntegrationTest.updateProductCountOnProductCreated:31 » ConditionTimeout
Expecting any element of:
  []
to satisfy the given assertions requirements but none did within 5 seconds.
```
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Increase the Awaitility timeout in `CategoryStatsIntegrationTest` so the test tolerates Kafka consumer lag under CI load
- [ ] #2 Test passes consistently across multiple CI runs without timing out
<!-- AC:END -->
