---
id: TASK-358
title: Use one timestamp for the audit fields of a new aggregate
status: To Do
assignee: []
created_date: '2026-11-21 18:10'
labels:
  - audit
dependencies: []
priority: low
type: bug
ordinal: 197000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
On create, the generated service sets `createdAt` and `lastModifiedAt` with two separate `Instant.now()` calls
(`AuditFields`), so they differ by a few microseconds. A newly created aggregate should report
`lastModifiedAt == createdAt`, which is what clients use to tell "never modified".

Found while building the showcase catalog (TASK-337.03); `ProductAuditIntegrationTest` tolerates the difference with
`isCloseTo` until this is fixed and can then assert equality.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 createdAt and lastModifiedAt of a new aggregate are equal, also inside AuditInfo
- [ ] #2 ProductAuditIntegrationTest asserts equality
<!-- AC:END -->
