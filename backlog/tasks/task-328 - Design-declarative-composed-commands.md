---
id: TASK-328
title: Design declarative composed commands across aggregate operations
status: To Do
assignee: []
created_date: '2026-11-17 10:00'
labels: [spike, architecture, bff]
dependencies: []
priority: low
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Design how a frontend command could declare several `@Create`/`@Update` steps executed in one local transaction, instead of a handwritten orchestration service (see `ProductCommandService` in TASK-320). Must be honest about atomicity: Prefab's direct event dispatch is not an outbox, and cross-aggregate or cross-service workflows need an explicit outbox/saga policy.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Proposed declaration syntax compared with the handwritten prototype
- [ ] #2 Transaction, event-publication and compensation semantics specified
- [ ] #3 Decision recorded as an ADR; implementation stories created if viable
<!-- AC:END -->
