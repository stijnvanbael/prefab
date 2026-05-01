---
id: TASK-153
title: Fix flaky CategoryStatsIntegrationTest in mongodb-example
status: To Do
assignee: []
created_date: '2026-04-30 18:14'
updated_date: '2026-05-01 20:26'
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
CategoryStatsIntegrationTest.updateProductCountOnProductCreated intermittently fails. Root cause: race condition between Kafka consumers on different topics. CategoryCreated is on the category topic while ProductCreated is on the product topic. The ProductCreated consumer may process its message before the CategoryStats aggregate is created by the CategoryCreated consumer, causing findByCategory() to return an empty list, which throws IllegalStateException. With the test retry config (limit=1, initial-interval=10ms), the message goes to DLT before CategoryStats is created, so totalProducts never reaches 2 and the test times out. This race condition reproduces locally too, not just under CI load.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Increase the Awaitility timeout in `CategoryStatsIntegrationTest` so the test tolerates Kafka consumer lag under CI load
- [ ] #2 Test passes consistently across multiple CI runs without timing out
<!-- AC:END -->
