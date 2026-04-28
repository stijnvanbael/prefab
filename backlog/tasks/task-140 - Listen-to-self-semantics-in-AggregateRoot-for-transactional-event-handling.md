---
id: TASK-140
title: Listen-to-self semantics in AggregateRoot for transactional event handling
status: To Do
assignee: []
created_date: '2026-04-27 13:10'
updated_date: '2026-04-27 16:41'
labels:
  - ddd
  - aggregate
  - events
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Aggregate roots should support listen-to-self semantics by publishing domain events to an async event bus (e.g. Kafka) and processing them through an idempotent event listener. The database state is only mutated inside the async listener, ensuring exactly-once semantics via idempotency rather than tight transactional coupling. The aggregate raises an event; the framework publishes it to the configured broker; the generated event handler on the same service consumes it and applies the state change idempotently.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An aggregate root can raise a domain event that the framework publishes to the configured async broker (e.g. Kafka) immediately after the triggering operation
- [ ] #2 The database state is never mutated at the point of raising the event; all state changes happen exclusively inside the async event listener
- [ ] #3 The generated event listener on the same service consumes the published event and applies the state change idempotently (re-processing the same event twice produces the same result)
- [ ] #4 The idempotency mechanism is documented and testable (e.g. deduplication key derived from the event)
- [ ] #5 Integration tests verify the full round-trip: event raised → published to broker → consumed by listener → state persisted
- [ ] #6 Integration tests verify that replaying a duplicate event does not produce duplicate state changes
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
See decision-001 for listen-to-self implementation alternatives analysis.
<!-- SECTION:NOTES:END -->
