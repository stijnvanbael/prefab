---
id: TASK-333
title: Evaluate durable cross-service workflow orchestration
status: To Do
assignee: []
created_date: '2026-11-18 10:39'
labels:
  - spike
  - architecture
  - integration
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Assess a durable onboarding workflow spanning an identity provider, CRM and billing, including recovery and compensation without a shared distributed transaction. Distinguish this from local composed commands in TASK-328, job status in TASK-157, and existing event, messaging and any supported outbox mechanisms; verify actual runtime guarantees rather than assuming them. Related: TASK-309 and decision-006.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Compare handwritten coordination using current Prefab events and messaging with an isolated durable workflow example or prototype, and identify which repeated plumbing could be generalized.
- [ ] #2 Specify persisted state, correlation, idempotency, retries, timeouts, ordering, recovery, compensation failure and operator intervention, including explicit at-least-once and transaction boundaries.
- [ ] #3 Exercise interruption/restart, duplicate delivery, an unavailable dependency and failed compensation with tests or a reproducible fault-injection demonstration.
- [ ] #4 Document security and tenant ownership, observability and operational burden; distinguish a Prefab integration with an existing orchestrator from building a runtime.
- [ ] #5 Record a go/no-go recommendation and bounded follow-up slices only if justified; do not promise exactly-once execution or distributed atomicity.
<!-- AC:END -->
