---
id: TASK-332
title: Evaluate secure incoming webhook adapters
status: To Do
assignee: []
created_date: '2026-11-18 10:38'
labels:
  - spike
  - integration
  - security
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Assess a reusable ingress contract for provider webhooks, from raw request authentication through replay prevention and routing to application or event handlers. This is distinct from outgoing webhook delivery in TASK-110. TASK-309 and decision-006 identify webhook ingress as a potential integration gap, not an architecture commitment.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Compare a handwritten provider adapter with an isolated prototype for one real or representative signed webhook, identifying reusable versus provider-specific protocol rules.
- [ ] #2 Specify signature verification over the unmodified request body, secret management and rotation, timestamp tolerance, replay/deduplication scope, input limits and explicit rejection behavior.
- [ ] #3 Define acknowledgement, durable acceptance, retry and idempotency boundaries, and show invalid signature, duplicate delivery, expired request and handler failure behavior in tests.
- [ ] #4 Describe tenant identity and authorization, logging without secrets, supported provider/version limits and the relationship to existing event consumers and TASK-110.
- [ ] #5 Record a go/no-go recommendation and implementation slices only if repeated value is demonstrated; do not claim exactly-once delivery.
<!-- AC:END -->
