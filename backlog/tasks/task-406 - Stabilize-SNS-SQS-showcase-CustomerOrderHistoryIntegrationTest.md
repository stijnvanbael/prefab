---
id: TASK-406
title: Stabilize SNS/SQS showcase CustomerOrderHistoryIntegrationTest
status: To Do
assignee: []
created_date: '2026-11-28T09:31:13Z'
labels:
  - events
  - testing
dependencies: []
ordinal: 215000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
examples/showcase-sns-sqs CustomerOrderHistoryIntegrationTest times out in 3 of 4 tests when the module runs, standalone and in full reactor, on origin/main (verified pre-existing; not caused by TASK-385 Pub/Sub changes). Stale events from earlier tests target cleared tables, analogous to TASK-385 but on the SNS/SQS transport (SnsTestLifecycle, TestSqsSubscriberExecutionListener, SqsUtil retry/dead-letter). Investigate test isolation and subscriber failure handling for SNS/SQS without weakening production retry + dead-letter.
<!-- SECTION:DESCRIPTION:END -->
