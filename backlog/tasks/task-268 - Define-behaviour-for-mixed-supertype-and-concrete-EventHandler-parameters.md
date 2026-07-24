---
id: TASK-268
title: Define behaviour for mixed supertype and concrete @EventHandler parameters
status: To Do
assignee: []
created_date: '2026-07-24 10:35'
labels:
  - annotation-processor
  - events
  - kafka
  - pubsub
  - sns-sqs
dependencies:
  - TASK-266
priority: medium
---

## Description

Clarify and implement the intended runtime/dispatch semantics when multiple `@EventHandler` methods for the same topic mix a shared event contract type (interface or sealed supertype) with one or more concrete implementation types.

Current behaviour intentionally rejects this setup with a compile-time error to avoid generating dominated `switch` patterns, but framework behaviour for this use case is not yet defined.

## Acceptance Criteria

- [ ] #1 A documented contract defines how mixed supertype + concrete handlers should behave (allowed pattern(s), invocation order, and duplication rules)
- [ ] #2 Processor generation follows the documented contract consistently across Kafka, Pub/Sub, and SNS/SQS consumers
- [ ] #3 Regression tests cover valid and invalid mixed-type combinations, including create/update aggregate handler variants where relevant
- [ ] #4 Documentation (`annotation-reference`, troubleshooting, and at least one guide/example) reflects the final supported behaviour and migration guidance

## Implementation Notes

- Track current stop-gap behaviour introduced in `TASK-266` follow-up:
  - clear compiler error: `Mixed @EventHandler parameter hierarchy is not supported ...`
- Revisit whether dispatch should:
  - prefer concrete handlers over supertype handlers,
  - invoke both (and how to prevent duplicate side effects), or
  - require an explicit opt-in annotation/strategy.
- If semantics materially change generated code shape, record design decision in `backlog/decisions/`.

