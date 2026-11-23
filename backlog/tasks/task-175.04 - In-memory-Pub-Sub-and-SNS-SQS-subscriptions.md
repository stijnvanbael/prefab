---
id: TASK-175.04
title: In-memory Pub/Sub and SNS/SQS subscriptions
status: To Do
assignee: []
created_date: '2026-11-23 11:00'
labels:
  - test
dependencies:
  - TASK-175.03
parent_task_id: TASK-175
priority: high
type: task
ordinal: 156400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Cover Pub/Sub and SNS/SQS with the in-memory bus (TASK-175 design §4-5, decision-011). Generated subscribers depend on the concrete `PubSubUtil`/`SqsUtil`, which need cloud clients.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Narrow subscription interfaces are extracted from `PubSubUtil` and `SqsUtil`, implemented by both, and generated subscribers depend on the interfaces
- [ ] #2 Header/attribute stamping and reading move into per-platform message codecs in `core`, used by the real publishers/subscribers and by the bus, without behaviour change for the real transports
- [ ] #3 In in-memory mode, Pub/Sub and SQS subscribers and `@TestEventConsumer` fields receive events through the bus, serialized with the platform serializers
- [ ] #4 The Pub/Sub and SNS/SQS examples run in memory, except tests marked `containers = true`
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
